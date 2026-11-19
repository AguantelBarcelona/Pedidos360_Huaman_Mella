package cl.pedidos360.consumidores_kinesis_service.eventos;

public record InventarioKinesisEvent(
        String eventId,
        String eventType,
        Long productoId,
        Integer stockDisponible,
        Integer stockReservado,
        String origen,
        String fechaEvento
) {}
