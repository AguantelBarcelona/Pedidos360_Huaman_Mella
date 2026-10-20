package cl.pedidos360.consumidores_service.documentos;

import jakarta.persistence.*;

import java.time.Instant;

/** Comprobante generado para un pedido. */
@Entity
@Table(name = "documentos")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "numero", length = 40, nullable = false)
    private String numero;

    @Column(name = "tipo", length = 40, nullable = false)
    private String tipo;

    @Column(name = "contenido", columnDefinition = "text", nullable = false)
    private String contenido;

    @Column(name = "generado_en", nullable = false)
    private Instant generadoEn;

    protected Documento() { }

    public Documento(String eventId, Long pedidoId, String numero, String contenido) {
        this.eventId = eventId;
        this.pedidoId = pedidoId;
        this.numero = numero;
        this.tipo = "COMPROBANTE_PEDIDO";
        this.contenido = contenido;
        this.generadoEn = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public Long getPedidoId() { return pedidoId; }
    public String getNumero() { return numero; }
    public String getTipo() { return tipo; }
    public String getContenido() { return contenido; }
    public Instant getGeneradoEn() { return generadoEn; }
}
