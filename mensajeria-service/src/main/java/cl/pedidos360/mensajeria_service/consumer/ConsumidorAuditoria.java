package cl.pedidos360.mensajeria_service.consumer;

import cl.pedidos360.mensajeria_service.service.ControlConsumidores;
import cl.pedidos360.mensajeria_service.service.RegistroMensajesProcesados;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(
        name = "mensajeria.enabled",
        havingValue = "true"
)
public class ConsumidorAuditoria {

    private static final Logger log =
            LoggerFactory.getLogger(ConsumidorAuditoria.class);

    private final SqsClient sqs;
    private final JsonMapper jsonMapper;
    private final RegistroMensajesProcesados registro;
    private final ControlConsumidores control;

    private String queueUrl;

    public ConsumidorAuditoria(
            JsonMapper jsonMapper,
            RegistroMensajesProcesados registro,
            ControlConsumidores control,
            @Value("${aws.region:us-east-1}") String region) {

        this.jsonMapper = jsonMapper;
        this.registro = registro;
        this.control = control;

        this.sqs = SqsClient.builder()
                .region(Region.of(region))
                .build();
    }

    @PostConstruct
    public void iniciar() {

        queueUrl = sqs.getQueueUrl(
                GetQueueUrlRequest.builder()
                        .queueName("pedidos360-auditoria")
                        .build()
        ).queueUrl();

        log.info("Consumidor iniciado cola=pedidos360-auditoria");
    }

    @Scheduled(fixedDelayString = "${mensajeria.poll-delay-ms:5000}")
    public void consultarMensajes() {

        // No consultar nuevos mensajes si auditoria esta pausada.
        if (!control.estaActivo("auditoria")) {
            return;
        }

        try {
            var respuesta = sqs.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(5)
                            .waitTimeSeconds(10)
                            .visibilityTimeout(60)
                            .messageSystemAttributeNames(
                                    MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT
                            )
                            .build()
            );

            for (Message mensaje : respuesta.messages()) {
                procesar(mensaje);
            }

        } catch (Exception e) {
            log.error(
                    "SQS_CONSULTA_ERROR cola=auditoria razon={}",
                    e.getMessage(),
                    e
            );
        }
    }

    private void procesar(Message mensaje) {

        String intento = mensaje.attributes().getOrDefault(
                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                "1"
        );

        String eventId = "desconocido";

        try {
            JsonNode evento = extraerEvento(mensaje.body());

            eventId = evento.path("eventId").asText();
            String eventType = evento.path("eventType").asText();

            if (eventId.isBlank()) {
                throw new IllegalArgumentException(
                        "El evento no tiene eventId"
                );
            }

            String detalle;

            if ("PEDIDO_CREADO".equals(eventType)) {

                long pedidoId = evento.path("pedidoId").asLong();

                if (pedidoId <= 0) {
                    throw new IllegalArgumentException(
                            "pedidoId invalido"
                    );
                }

                detalle = "pedidoId=" + pedidoId;

            } else if ("PRODUCTO_ACTUALIZADO".equals(eventType)) {

                long productoId = evento.path("productoId").asLong();

                if (productoId <= 0) {
                    throw new IllegalArgumentException(
                            "productoId invalido"
                    );
                }

                detalle = "productoId=" + productoId;

            } else {

                throw new IllegalArgumentException(
                        "Tipo de evento no soportado: " + eventType
                );
            }

            boolean esNuevo = registro.registrarSiNuevo(
                    eventId,
                    "auditoria",
                    eventType,
                    mensaje.messageId()
            );

            if (esNuevo) {

                log.info(
                        "AUDITORIA_PROCESADA eventId={} tipo={} {} intento={} messageId={}",
                        eventId,
                        eventType,
                        detalle,
                        intento,
                        mensaje.messageId()
                );

            } else {

                log.info(
                        "SQS_DUPLICADO cola=auditoria eventId={} intento={} messageId={}",
                        eventId,
                        intento,
                        mensaje.messageId()
                );
            }

            // ACK solo despues del registro correcto.
            sqs.deleteMessage(
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(mensaje.receiptHandle())
                            .build()
            );

            log.info(
                    "SQS_ACK cola=auditoria eventId={} messageId={}",
                    eventId,
                    mensaje.messageId()
            );

        } catch (Exception e) {

            // Sin ACK para permitir reintentos y posterior DLQ.
            log.error(
                    "SQS_ERROR cola=auditoria eventId={} messageId={} intento={} razon={}",
                    eventId,
                    mensaje.messageId(),
                    intento,
                    e.getMessage()
            );
        }
    }

    private JsonNode extraerEvento(String cuerpo) throws Exception {

        JsonNode raiz = jsonMapper.readTree(cuerpo);

        // Mensaje con envoltorio SNS.
        if ("Notification".equals(raiz.path("Type").asText())
                && !raiz.path("Message").isMissingNode()) {

            return jsonMapper.readTree(
                    raiz.path("Message").asText()
            );
        }

        // Mensaje directo o Raw Message Delivery.
        return raiz;
    }

    @PreDestroy
    public void cerrar() {
        sqs.close();
    }
}
