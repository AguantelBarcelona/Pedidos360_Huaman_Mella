package cl.pedidos360.consumidores_service.auditoria;

import jakarta.persistence.*;

import java.time.Instant;

/** Traza inmutable de todo evento de negocio publicado en el sistema. */
@Entity
@Table(name = "registros_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "event_type", length = 80, nullable = false)
    private String eventType;

    @Column(name = "origen", length = 80)
    private String origen;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "fecha_evento", length = 40)
    private String fechaEvento;

    @Column(name = "payload", columnDefinition = "text", nullable = false)
    private String payload;

    @Column(name = "registrado_en", nullable = false)
    private Instant registradoEn;

    protected RegistroAuditoria() { }

    public RegistroAuditoria(String eventId, String eventType, String origen, Long pedidoId,
                             Long productoId, String fechaEvento, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.origen = origen;
        this.pedidoId = pedidoId;
        this.productoId = productoId;
        this.fechaEvento = fechaEvento;
        this.payload = payload;
        this.registradoEn = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getOrigen() { return origen; }
    public Long getPedidoId() { return pedidoId; }
    public Long getProductoId() { return productoId; }
    public String getFechaEvento() { return fechaEvento; }
    public String getPayload() { return payload; }
    public Instant getRegistradoEn() { return registradoEn; }
}
