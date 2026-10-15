package cl.pedidos360.pedidos_service.service;

public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(Long id) {
        super("No existe un pedido con id " + id);
    }
}