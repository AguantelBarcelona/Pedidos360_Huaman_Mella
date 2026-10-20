package cl.pedidos360.consumidores_service.notificaciones;

import jakarta.persistence.*;

import java.time.Instant;

/** Notificacion enviada al cliente (canal simulado: se registra y se escribe en el log). */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "destinatario", nullable = false)
    private String destinatario;

    @Column(name = "asunto", nullable = false)
    private String asunto;

    @Column(name = "mensaje", length = 1000, nullable = false)
    private String mensaje;

    @Column(name = "canal", length = 30, nullable = false)
    private String canal;

    @Column(name = "enviada_en", nullable = false)
    private Instant enviadaEn;

    protected Notificacion() { }

    public Notificacion(String eventId, Long pedidoId, String destinatario, String asunto, String mensaje) {
        this.eventId = eventId;
        this.pedidoId = pedidoId;
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.canal = "EMAIL_SIMULADO";
        this.enviadaEn = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public Long getPedidoId() { return pedidoId; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public String getCanal() { return canal; }
    public Instant getEnviadaEn() { return enviadaEn; }
}
