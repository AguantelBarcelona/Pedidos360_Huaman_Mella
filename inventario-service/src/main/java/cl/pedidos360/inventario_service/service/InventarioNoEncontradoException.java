package cl.pedidos360.inventario_service.service;

public class InventarioNoEncontradoException
        extends RuntimeException {

    public InventarioNoEncontradoException(Long productoId) {

        super("No existe inventario para el producto "
                + productoId);
    }
}