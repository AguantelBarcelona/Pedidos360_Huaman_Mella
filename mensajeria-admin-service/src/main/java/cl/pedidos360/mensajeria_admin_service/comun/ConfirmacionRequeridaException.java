package cl.pedidos360.mensajeria_admin_service.comun;

public class ConfirmacionRequeridaException extends RuntimeException {
    public ConfirmacionRequeridaException(String mensaje) {
        super(mensaje);
    }
}
