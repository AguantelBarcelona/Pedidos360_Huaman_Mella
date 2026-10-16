package cl.pedidos360.consumidores_service.comun.sqs;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.errores.ErrorRecuperableException;
import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;
import cl.pedidos360.consumidores_service.comun.fallos.RegistroFallo;
import cl.pedidos360.consumidores_service.comun.fallos.RegistroFalloRepository;
import cl.pedidos360.consumidores_service.comun.fallos.SimuladorFallos;
import cl.pedidos360.consumidores_service.comun.idempotencia.TransaccionConsumo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class ConsumidorSqs {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorSqs.class);

    private final String dominio;
    private final String colaUrl;
    private final String dlqUrl;
    private final ProcesadorEvento procesador;
    private final SqsClient sqs;
    private final JsonMapper json;
    private final TransaccionConsumo transaccion;
    private final RegistroFalloRepository fallos;
    private final SimuladorFallos simulador;
    private final ConsumidoresProperties props;

    private final AtomicBoolean ejecutando = new AtomicBoolean(false);
    private final AtomicBoolean pausado = new AtomicBoolean(false);
    private final AtomicLong procesados = new AtomicLong();
    private final AtomicLong duplicados = new AtomicLong();
    private final AtomicLong fallosRecuperables = new AtomicLong();
    private final AtomicLong enviadosDlq = new AtomicLong();
    private volatile Instant ultimoMensaje;
    private volatile String ultimoError;
    private Thread hilo;

    public ConsumidorSqs(String dominio, String colaUrl, String dlqUrl, ProcesadorEvento procesador,
                         SqsClient sqs, JsonMapper json, TransaccionConsumo transaccion,
                         RegistroFalloRepository fallos, SimuladorFallos simulador,
                         ConsumidoresProperties props) {
        this.dominio = dominio;
        this.colaUrl = colaUrl;
        this.dlqUrl = dlqUrl;
        this.procesador = procesador;
        this.sqs = sqs;
        this.json = json;
        this.transaccion = transaccion;
        this.fallos = fallos;
        this.simulador = simulador;
        this.props = props;
    }

    public synchronized void iniciar() {
        if (ejecutando.getAndSet(true)) {
            return;
        }
        hilo = new Thread(this::bucle, "sqs-" + dominio);
        hilo.start();
        log.info("consumidor={} instancia={} INICIADO cola={}", dominio, props.instancia(), colaUrl);
    }

    public synchronized void detener() {
        if (!ejecutando.getAndSet(false)) {
            return;
        }
        if (hilo != null) {
            hilo.interrupt();
            try {
                hilo.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("consumidor={} instancia={} DETENIDO", dominio, props.instancia());
    }

    public void pausar() {
        if (!pausado.getAndSet(true)) {
            log.info("consumidor={} instancia={} PAUSADO", dominio, props.instancia());
        }
    }

    public void reanudar() {
        if (pausado.getAndSet(false)) {
            log.info("consumidor={} instancia={} REANUDADO", dominio, props.instancia());
        }
    }

    private void bucle() {
        while (ejecutando.get()) {
            if (pausado.get()) {
                dormir(1_000);
                continue;
            }
            try {
                recibirYProcesar();
            } catch (RuntimeException e) {
                if (!ejecutando.get()) {
                    break; 
                }
                ultimoError = Instant.now() + " " + e.getMessage();
                log.error("consumidor={} instancia={} error leyendo cola: {}", dominio, props.instancia(), e.getMessage());
                dormir(5_000);
            }
        }
    }

    int recibirYProcesar() {
        List<Message> mensajes = sqs.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(colaUrl)
                .maxNumberOfMessages(props.maxMensajes())
                .waitTimeSeconds(props.esperaSegundos())
                .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                        MessageSystemAttributeName.SENT_TIMESTAMP)
                .messageAttributeNames("All")
                .build()).messages();
        for (Message mensaje : mensajes) {
            procesarMensaje(mensaje);
        }
        return mensajes.size();
    }

    void procesarMensaje(Message mensaje) {
        ultimoMensaje = Instant.now();
        int intento = intento(mensaje);
        String eventId = null;
        try {
            EventoMensaje evento = EventoMensaje.desdeJson(mensaje.body(), atributo(mensaje, "eventType"), json);
            eventId = evento.eventId();

            simulador.aplicar(dominio);

            boolean nuevo;
            try {
                nuevo = transaccion.procesarUnaVez(procesador, evento, mensaje.messageId(), props.instancia(), intento);
            } catch (DataIntegrityViolationException carrera) {
                if (!transaccion.yaProcesado(dominio, evento.eventId())) {
                    throw new ErrorRecuperableException("Error de integridad en BD: " + carrera.getMostSpecificCause().getMessage(), carrera);
                }
                nuevo = false;
            }

            ack(mensaje);
            if (nuevo) {
                procesados.incrementAndGet();
                log.info("consumidor={} instancia={} messageId={} eventId={} tipo={} intento={} resultado=PROCESADO_ACK",
                        dominio, props.instancia(), mensaje.messageId(), eventId, evento.eventType(), intento);
            } else {
                duplicados.incrementAndGet();
                log.info("consumidor={} instancia={} messageId={} eventId={} intento={} resultado=DUPLICADO_IGNORADO_ACK",
                        dominio, props.instancia(), mensaje.messageId(), eventId, intento);
            }
        } catch (ErrorDefinitivoException e) {
            manejarDefinitivo(mensaje, eventId, intento, e.getMessage());
        } catch (ErrorRecuperableException e) {
            manejarRecuperable(mensaje, eventId, intento, e.getMessage());
        } catch (RuntimeException e) {
            manejarRecuperable(mensaje, eventId, intento, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void manejarRecuperable(Message mensaje, String eventId, int intento, String causa) {
        fallosRecuperables.incrementAndGet();
        boolean ultimo = intento >= props.maxIntentos();
        int espera = Math.min(props.backoffBaseSegundos() * intento, 900);
        String accion = ultimo
                ? "SIN_ACK; agotados " + props.maxIntentos() + " intentos, SQS lo movera a la DLQ (redrive)"
                : "SIN_ACK; reintento en " + espera + "s";

        log.warn("consumidor={} instancia={} messageId={} eventId={} intento={}/{} resultado=ERROR_RECUPERABLE causa=\"{}\" accion=\"{}\"",
                dominio, props.instancia(), mensaje.messageId(), eventId, intento, props.maxIntentos(), causa, accion);
        registrarFallo(mensaje, eventId, intento, RegistroFallo.Tipo.RECUPERABLE, causa, accion);

        try {
            sqs.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                    .queueUrl(colaUrl)
                    .receiptHandle(mensaje.receiptHandle())
                    .visibilityTimeout(espera)
                    .build());
        } catch (RuntimeException e) {
            log.debug("No se pudo ajustar visibilidad: {}", e.getMessage());
        }
    }

    private void manejarDefinitivo(Message mensaje, String eventId, int intento, String causa) {
        if (dlqUrl == null || dlqUrl.isBlank()) {
            manejarRecuperable(mensaje, eventId, intento, "Error definitivo sin DLQ configurada: " + causa);
            return;
        }
        try {
            Map<String, MessageAttributeValue> atributos = new HashMap<>(mensaje.messageAttributes());
            atributos.put("motivoFallo", texto(causa.length() > 250 ? causa.substring(0, 250) : causa));
            atributos.put("dominioOrigen", texto(dominio));
            atributos.put("messageIdOriginal", texto(mensaje.messageId()));
            atributos.put("tipoFallo", texto("DEFINITIVO"));

            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .messageBody(mensaje.body())
                    .messageAttributes(atributos)
                    .build());
            ack(mensaje); 
            enviadosDlq.incrementAndGet();

            String accion = "ENVIADO_A_DLQ + ACK";
            log.error("consumidor={} instancia={} messageId={} eventId={} intento={} resultado=ERROR_DEFINITIVO causa=\"{}\" accion=\"{}\"",
                    dominio, props.instancia(), mensaje.messageId(), eventId, intento, causa, accion);
            registrarFallo(mensaje, eventId, intento, RegistroFallo.Tipo.DEFINITIVO, causa, accion);
        } catch (RuntimeException e) {
            manejarRecuperable(mensaje, eventId, intento, "No se pudo enviar a DLQ (" + e.getMessage() + "); causa original: " + causa);
        }
    }

    private void ack(Message mensaje) {
        sqs.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(colaUrl)
                .receiptHandle(mensaje.receiptHandle())
                .build());
    }

    private void registrarFallo(Message m, String eventId, int intento, RegistroFallo.Tipo tipo, String causa, String accion) {
        ultimoError = Instant.now() + " " + tipo + " " + causa;
        try {
            fallos.save(new RegistroFallo(dominio, m.messageId(), eventId, intento, tipo, causa, accion, props.instancia()));
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar registro_fallos (queda solo en log): {}", e.getMessage());
        }
    }

    static int intento(Message mensaje) {
        String valor = mensaje.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT);
        try {
            return valor == null ? 1 : Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String atributo(Message mensaje, String nombre) {
        MessageAttributeValue valor = mensaje.messageAttributes().get(nombre);
        return valor == null ? null : valor.stringValue();
    }

    private static MessageAttributeValue texto(String valor) {
        return MessageAttributeValue.builder().dataType("String").stringValue(valor == null ? "-" : valor).build();
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ejecutando.set(false);
        }
    }

    public String dominio() { return dominio; }

    public EstadoConsumidor estado() {
        return new EstadoConsumidor(dominio, props.instancia(), colaUrl, dlqUrl,
                ejecutando.get(), pausado.get(), simulador.modo(dominio).name(),
                procesados.get(), duplicados.get(), fallosRecuperables.get(), enviadosDlq.get(),
                ultimoMensaje, ultimoError);
    }

    public record EstadoConsumidor(String dominio, String instancia, String colaUrl, String dlqUrl,
                                   boolean ejecutando, boolean pausado, String simulacionFallo,
                                   long procesados, long duplicados, long fallosRecuperables,
                                   long enviadosDlq, Instant ultimoMensaje, String ultimoError) {
    }
}
