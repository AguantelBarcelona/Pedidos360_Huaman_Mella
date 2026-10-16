package cl.pedidos360.consumidores_service.comun.evento;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.errores.ErrorRecuperableException;

/**
 * Debe lanzar {@link ErrorDefinitivoException} si el evento nunca podra procesarse,
 * o {@link ErrorRecuperableException} si la falla es temporal.
 */
public interface ProcesadorEvento {

    
    String dominio();

    void procesar(EventoMensaje evento);
}
