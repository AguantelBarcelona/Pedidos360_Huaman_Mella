package cl.pedidos360.inventario_service.kinesis;

public record InventarioKinesisEvent(
        String eventId,
        String eventType,
        Long productoId,
        Integer stockDisponible,
        Integer stockReservado,
        String origen,
        String fechaEvento
) {}
