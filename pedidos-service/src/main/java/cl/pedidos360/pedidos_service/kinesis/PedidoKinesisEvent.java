package cl.pedidos360.pedidos_service.kinesis;

import java.math.BigDecimal;

public record PedidoKinesisEvent(
        String eventId,
        String eventType,
        Long pedidoId,
        Long productoId,
        Integer cantidad,
        BigDecimal total,
        String estado,
        String origen,
        String fechaEvento
) {}