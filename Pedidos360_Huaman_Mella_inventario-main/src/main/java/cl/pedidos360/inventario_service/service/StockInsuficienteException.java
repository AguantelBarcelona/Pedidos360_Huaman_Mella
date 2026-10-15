package cl.pedidos360.inventario_service.service;

public class StockInsuficienteException
        extends RuntimeException {

    public StockInsuficienteException(Long productoId) {

        super("Stock insuficiente para el producto "
                + productoId);
    }
}