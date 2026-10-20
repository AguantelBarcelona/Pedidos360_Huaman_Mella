package cl.pedidos360.consumidores_service.comun.evento;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.errores.ErrorRecuperableException;
import cl.pedidos360.consumidores_service.comun.sqs.EscuchaSqs;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Logica de negocio de un dominio (notificaciones, documentos, auditoria).
 * Cada implementacion se anota con {@link EscuchaSqs} para indicar que cola escucha.
 * Se ejecuta dentro de la misma transaccion que marca el evento como procesado.
 *
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
