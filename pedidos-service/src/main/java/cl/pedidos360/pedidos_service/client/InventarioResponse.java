package cl.pedidos360.pedidos_service.client;

public record InventarioResponse(
        Long id,
        Long productoId,
        Integer stockDisponible,
        Integer stockReservado
) {
}