package cl.pedidos360.consumidores_service.comun.sqs;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase como consumidor SQS de un dominio funcional
 * (equivalente a {@code @SqsListener} de Spring Cloud AWS).
 *
 * <pre>
 * &#64;EscuchaSqs(dominio = "notificaciones")
 * public class NotificacionProcesador implements ProcesadorEvento { ... }
 * </pre>
 *
 * Al arrancar, {@link RegistroConsumidores} busca todos los beans con esta anotacion
 * y les conecta la cola configurada en {@code app.consumidores.colas.<dominio>}
 * (URL y DLQ en application.properties). La clase anotada solo contiene logica
 * de negocio: el ACK, los reintentos y el envio a la DLQ los maneja {@link ConsumidorSqs}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface EscuchaSqs {

    /** Dominio funcional; debe coincidir con la clave en app.consumidores.colas. */
    String dominio();
}
