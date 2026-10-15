package cl.pedidos360.pedidos_service.client;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String modelo,
        String marca,
        Integer talla,
        BigDecimal precio,
        String categoria,
        String descripcion
) {
}