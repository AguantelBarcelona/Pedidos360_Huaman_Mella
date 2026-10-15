package cl.pedidos360.pedidos_service.messaging;

import cl.pedidos360.pedidos_service.model.Pedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EventoOutboxService {

    private final EventoOutboxRepository repository;
    private final JsonMapper jsonMapper;
    private final boolean habilitado;

    public EventoOutboxService(EventoOutboxRepository repository,
                               JsonMapper jsonMapper,
                               @Value("${app.messaging.enabled:false}") boolean habilitado) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.habilitado = habilitado;
    }

    // Invocado dentro de @Transactional de PedidoService.crear(): si falla
    // la insercion del evento, se revierte tambien la insercion del pedido.
    public void registrarPedidoCreado(Pedido pedido) {
        if (!habilitado) {
            return;
        }
        if (pedido.getId() == null) {
            throw new IllegalStateException("El pedido debe tener ID antes de generar el evento");
        }
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("eventId", eventId);
        mensaje.put("eventType", "PEDIDO_CREADO");
        mensaje.put("pedidoId", pedido.getId());
        mensaje.put("productoId", pedido.getProductoId());
        mensaje.put("cantidad", pedido.getCantidad());
        mensaje.put("clienteEmail", pedido.getClienteEmail());
        mensaje.put("origen", "pedidos-service");
        mensaje.put("fechaEvento", Instant.now().toString());

        repository.save(new EventoOutbox(eventId, "PEDIDO_CREADO", jsonMapper.writeValueAsString(mensaje)));
    }
}
