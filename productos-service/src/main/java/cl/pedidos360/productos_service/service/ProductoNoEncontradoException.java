package cl.pedidos360.productos_service.service;

public class ProductoNoEncontradoException
        extends RuntimeException {

    public ProductoNoEncontradoException(Long id) {

        super("No existe un producto con id " + id);
    }
}