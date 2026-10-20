package cl.pedidos360.consumidores_service.comun.idempotencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro de idempotencia: un evento se procesa una sola vez POR DOMINIO.
 * La clave primaria (dominio:eventId) impide que dos instancias, o una
 * re-entrega de SQS, ejecuten dos veces el mismo efecto de negocio.
 */
@Entity
@Table(name = "eventos_procesados")
public class EventoProcesado {

    @Id
    @Column(name = "id", length = 120, nullable = false)
    private String id;

    @Column(name = "dominio", length = 40, nullable = false)
    private String dominio;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "event_type", length = 80, nullable = false)
    private String eventType;

    @Column(name = "message_id", length = 100)
    private String messageId;

    @Column(name = "instancia", length = 80)
    private String instancia;

    @Column(name = "intento")
    private int intento;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;

    protected EventoProcesado() { }

    public EventoProcesado(String dominio, String eventId, String eventType,
                           String messageId, String instancia, int intento) {
        this.id = clave(dominio, eventId);
        this.dominio = dominio;
        this.eventId = eventId;
        this.eventType = eventType;
        this.messageId = messageId;
        this.instancia = instancia;
        this.intento = intento;
        this.procesadoEn = Instant.now();
    }

    public static String clave(String dominio, String eventId) {
        return dominio + ":" + eventId;
    }

    public String getId() { return id; }
    public String getDominio() { return dominio; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getMessageId() { return messageId; }
    public String getInstancia() { return instancia; }
    public int getIntento() { return intento; }
    public Instant getProcesadoEn() { return procesadoEn; }
}
