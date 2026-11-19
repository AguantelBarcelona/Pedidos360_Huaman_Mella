package cl.pedidos360.consumidores_kinesis_service.eventos;

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
