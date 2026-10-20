package cl.pedidos360.consumidores_service.comun.errores;

/**
 * Falla temporal (BD caida, servicio externo no responde, timeout...).
 * El mensaje NO se confirma: vuelve a la cola y se reintenta. Al superar
 * maxReceiveCount la RedrivePolicy de SQS lo mueve a la DLQ.
 */
public class ErrorRecuperableException extends RuntimeException {
    public ErrorRecuperableException(String mensaje) {
        super(mensaje);
    }

    public ErrorRecuperableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
