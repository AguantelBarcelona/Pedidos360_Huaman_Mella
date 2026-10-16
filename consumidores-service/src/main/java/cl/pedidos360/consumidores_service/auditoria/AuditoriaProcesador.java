package cl.pedidos360.consumidores_service.auditoria;

import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Dominio auditoria: registra todos los tipos de evento (PEDIDO_CREADO,
 * PEDIDO_CANCELADO, PRODUCTO_ACTUALIZADO). Guarda el JSON completo.
 */
@Component
public class AuditoriaProcesador implements ProcesadorEvento {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaProcesador.class);
    private final RegistroAuditoriaRepository repository;

    public AuditoriaProcesador(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public String dominio() {
        return "auditoria";
    }

    @Override
    public void procesar(EventoMensaje evento) {
        repository.save(new RegistroAuditoria(evento.eventId(), evento.eventType(), evento.origen(),
                evento.pedidoId(), evento.productoId(), evento.fechaEvento(), evento.payload()));
        log.info("Auditoria registrada eventId={} tipo={} origen={}", evento.eventId(), evento.eventType(), evento.origen());
    }
}
