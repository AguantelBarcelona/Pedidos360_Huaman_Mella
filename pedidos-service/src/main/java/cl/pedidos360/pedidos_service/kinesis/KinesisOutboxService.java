package cl.pedidos360.pedidos_service.kinesis;

import cl.pedidos360.pedidos_service.model.Pedido;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class KinesisOutboxService {

    private final KinesisOutboxRepository repository;
    private final JsonMapper mapper;
    private final boolean habilitado;

    public KinesisOutboxService(
            KinesisOutboxRepository repository,
            JsonMapper mapper,
            @Value("${app.kinesis.enabled:false}") boolean habilitado) {

        this.repository = repository;
        this.mapper = mapper;
        this.habilitado = habilitado;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarPedidoCreado(Pedido pedido) {

        if (!habilitado) {
            return;
        }

        String eventId = UUID.randomUUID().toString();

        String partitionKey = Objects.requireNonNull(
                pedido.getId(), "pedidoId requerido").toString();

        PedidoKinesisEvent evento = new PedidoKinesisEvent(
                eventId,
                "PEDIDO_CREADO",
                pedido.getId(),
                pedido.getProductoId(),
                pedido.getCantidad(),
                pedido.getTotal(),
                pedido.getEstado(),
                "pedidos-service",
                Instant.now().toString()
        );

        repository.save(new KinesisOutboxEvent(
                eventId,
                evento.eventType(),
                partitionKey,
                mapper.writeValueAsString(evento)
        ));
    }
}