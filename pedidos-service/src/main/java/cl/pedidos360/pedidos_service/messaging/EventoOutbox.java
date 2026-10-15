package cl.pedidos360.pedidos_service.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Un registro por evento de negocio, dentro de la transaccion del pedido. */
@Entity
@Table(name = "eventos_outbox", schema = "pedidos")
public class EventoOutbox {

    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "event_type", length = 80, nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "text", nullable = false)
    private String payload;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "publicado_en")
    private Instant publicadoEn;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    protected EventoOutbox() { }

    public EventoOutbox(String eventId, String eventType, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.estado = "PENDIENTE";
        this.intentos = 0;
        this.creadoEn = Instant.now();
    }

    public void publicado() {
        this.estado = "PUBLICADO";
        this.publicadoEn = Instant.now();
        this.ultimoError = null;
    }

    public void error(Throwable causa) {
        this.intentos++;
        String mensaje = causa.getMessage() == null ? causa.getClass().getSimpleName() : causa.getMessage();
        this.ultimoError = mensaje.substring(0, Math.min(mensaje.length(), 500));
        this.estado = this.intentos >= 10 ? "ERROR" : "PENDIENTE";
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getEstado() { return estado; }
    public int getIntentos() { return intentos; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getPublicadoEn() { return publicadoEn; }
    public String getUltimoError() { return ultimoError; }
}
