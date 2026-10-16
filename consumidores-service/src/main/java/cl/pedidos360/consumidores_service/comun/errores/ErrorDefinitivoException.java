package cl.pedidos360.consumidores_service.comun.errores;


public class ErrorDefinitivoException extends RuntimeException {
    public ErrorDefinitivoException(String mensaje) {
        super(mensaje);
    }

    public ErrorDefinitivoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
