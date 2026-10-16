package cl.pedidos360.consumidores_service.comun.errores;


public class ErrorRecuperableException extends RuntimeException {
    public ErrorRecuperableException(String mensaje) {
        super(mensaje);
    }

    public ErrorRecuperableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
