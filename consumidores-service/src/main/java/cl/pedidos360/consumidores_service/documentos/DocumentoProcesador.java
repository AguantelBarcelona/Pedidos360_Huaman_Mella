package cl.pedidos360.consumidores_service.documentos;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DocumentoProcesador implements ProcesadorEvento {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProcesador.class);
    private final DocumentoRepository repository;

    public DocumentoProcesador(DocumentoRepository repository) {
        this.repository = repository;
    }

    @Override
    public String dominio() {
        return "documentos";
    }

    @Override
    public void procesar(EventoMensaje evento) {
        if (!"PEDIDO_CREADO".equals(evento.eventType())) {
            throw new ErrorDefinitivoException("Tipo de evento no soportado por documentos: " + evento.eventType());
        }
        if (evento.pedidoId() == null || evento.productoId() == null || evento.cantidad() == null) {
            throw new ErrorDefinitivoException("Evento PEDIDO_CREADO incompleto: faltan pedidoId, productoId o cantidad");
        }
        String numero = String.format("COMP-%08d", evento.pedidoId());
        String contenido = """
                COMPROBANTE DE PEDIDO %s
                Pedido: %d
                Cliente: %s
                Producto: %d
                Cantidad: %d
                Fecha del evento: %s
                Evento: %s
                """.formatted(numero, evento.pedidoId(), evento.clienteEmail(), evento.productoId(),
                evento.cantidad(), evento.fechaEvento(), evento.eventId());
        repository.save(new Documento(evento.eventId(), evento.pedidoId(), numero, contenido));
        log.info("Documento {} generado para pedido {}", numero, evento.pedidoId());
    }
}
