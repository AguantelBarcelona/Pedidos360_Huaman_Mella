package cl.pedidos360.consumidores_service.comun.errores;

/**
 * Falla que no se corrige reintentando (JSON invalido, datos obligatorios
 * ausentes, tipo de evento no soportado). El consumidor envia el mensaje
 * a la DLQ inmediatamente y luego confirma (ACK) el original.
 */
public class ErrorDefinitivoException extends RuntimeException {
    public ErrorDefinitivoException(String mensaje) {
        super(mensaje);
    }

    public ErrorDefinitivoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
