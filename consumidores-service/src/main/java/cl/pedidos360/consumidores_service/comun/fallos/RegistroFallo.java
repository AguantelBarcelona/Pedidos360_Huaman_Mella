package cl.pedidos360.consumidores_service.comun.fallos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Bitacora persistente de cada fallo: ID, intento y causa (requisito de la pauta). */
@Entity
@Table(name = "registro_fallos")
public class RegistroFallo {

    public enum Tipo { RECUPERABLE, DEFINITIVO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dominio", length = 40, nullable = false)
    private String dominio;

    @Column(name = "message_id", length = 100)
    private String messageId;

    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "intento", nullable = false)
    private int intento;

    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo;

    @Column(name = "causa", length = 500, nullable = false)
    private String causa;

    @Column(name = "accion", length = 120, nullable = false)
    private String accion;

    @Column(name = "instancia", length = 80)
    private String instancia;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    protected RegistroFallo() { }

    public RegistroFallo(String dominio, String messageId, String eventId, int intento,
                         Tipo tipo, String causa, String accion, String instancia) {
        this.dominio = dominio;
        this.messageId = messageId;
        this.eventId = eventId;
        this.intento = intento;
        this.tipo = tipo.name();
        String c = causa == null ? "sin detalle" : causa;
        this.causa = c.substring(0, Math.min(c.length(), 500));
        this.accion = accion;
        this.instancia = instancia;
        this.fecha = Instant.now();
    }

    public Long getId() { return id; }
    public String getDominio() { return dominio; }
    public String getMessageId() { return messageId; }
    public String getEventId() { return eventId; }
    public int getIntento() { return intento; }
    public String getTipo() { return tipo; }
    public String getCausa() { return causa; }
    public String getAccion() { return accion; }
    public String getInstancia() { return instancia; }
    public Instant getFecha() { return fecha; }
}
