package cl.pedidos360.consumidores_service.comun.evento;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.errores.ErrorRecuperableException;
import cl.pedidos360.consumidores_service.comun.sqs.EscuchaSqs;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Cada implementacion se anota con {@link EscuchaSqs} para indicar que cola escucha. *
 * Debe lanzar {@link ErrorDefinitivoException} si el evento nunca podra procesarse,
 * o {@link ErrorRecuperableException} si la falla es temporal.
 */
public interface ProcesadorEvento {

    /** Dominio declarado en {@link EscuchaSqs#dominio()}. */
    default String dominio() {
        EscuchaSqs anotacion = AnnotationUtils.findAnnotation(getClass(), EscuchaSqs.class);
        if (anotacion == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " debe anotarse con @EscuchaSqs");
        }
        return anotacion.dominio();
    }

    void procesar(EventoMensaje evento);
}
