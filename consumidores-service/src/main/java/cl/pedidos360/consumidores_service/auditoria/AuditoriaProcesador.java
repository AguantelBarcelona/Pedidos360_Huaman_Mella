package cl.pedidos360.consumidores_service.auditoria;

import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;
import cl.pedidos360.consumidores_service.comun.sqs.EscuchaSqs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dominio auditoria: registra todos los tipos de evento (PEDIDO_CREADO,
 * PEDIDO_CANCELADO, PRODUCTO_ACTUALIZADO). Guarda el JSON completo.
 */
@EscuchaSqs(dominio = "auditoria")
public class AuditoriaProcesador implements ProcesadorEvento {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaProcesador.class);
    private final RegistroAuditoriaRepository repository;

    public AuditoriaProcesador(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void procesar(EventoMensaje evento) {
        repository.save(new RegistroAuditoria(evento.eventId(), evento.eventType(), evento.origen(),
                evento.pedidoId(), evento.productoId(), evento.fechaEvento(), evento.payload()));
        log.info("Auditoria registrada eventId={} tipo={} origen={}", evento.eventId(), evento.eventType(), evento.origen());
    }
}
