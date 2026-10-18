package cl.pedidos360.mensajeria_admin_service.comun;

public class AccesoDenegadoRecursoException extends RuntimeException {
    public AccesoDenegadoRecursoException(String mensaje) {
        super(mensaje);
    }
}
