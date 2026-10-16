package cl.pedidos360.consumidores_service.notificaciones;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificacionProcesador implements ProcesadorEvento {

    private static final Logger log = LoggerFactory.getLogger(NotificacionProcesador.class);
    private final NotificacionRepository repository;

    public NotificacionProcesador(NotificacionRepository repository) {
        this.repository = repository;
    }

    @Override
    public String dominio() {
        return "notificaciones";
    }

    @Override
    public void procesar(EventoMensaje evento) {
        if (evento.clienteEmail() == null || evento.clienteEmail().isBlank()) {
            throw new ErrorDefinitivoException("Evento sin clienteEmail: no hay destinatario");
        }
        String asunto;
        String mensaje;
        switch (evento.eventType()) {
            case "PEDIDO_CREADO" -> {
                asunto = "Pedido #" + evento.pedidoId() + " recibido";
                mensaje = "Hola, registramos tu pedido #" + evento.pedidoId() + " por " + evento.cantidad()
                        + " unidad(es) del producto " + evento.productoId() + ". Te avisaremos cualquier cambio.";
            }
            case "PEDIDO_CANCELADO" -> {
                asunto = "Pedido #" + evento.pedidoId() + " cancelado";
                mensaje = "Hola, tu pedido #" + evento.pedidoId() + " fue cancelado.";
            }
            default -> throw new ErrorDefinitivoException(
                    "Tipo de evento no soportado por notificaciones: " + evento.eventType());
        }
        repository.save(new Notificacion(evento.eventId(), evento.pedidoId(), evento.clienteEmail(), asunto, mensaje));
        log.info("[EMAIL SIMULADO] para={} asunto=\"{}\"", evento.clienteEmail(), asunto);
    }
}
