package cl.pedidos360.pedidos_service.kinesis;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "eventos_kinesis_outbox", schema = "pedidos")
public class KinesisOutboxEvent {

    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "event_type", length = 80, nullable = false)
    private String eventType;

    @Column(name = "partition_key", length = 256, nullable = false)
    private String partitionKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "publicado_en")
    private Instant publicadoEn;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "shard_id", length = 100)
    private String shardId;

    @Column(name = "sequence_number", length = 200)
    private String sequenceNumber;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    protected KinesisOutboxEvent() {}

    public KinesisOutboxEvent(
            String eventId,
            String eventType,
            String partitionKey,
            String payload) {

        this.eventId = eventId;
        this.eventType = eventType;
        this.partitionKey = partitionKey;
        this.payload = payload;
        this.creadoEn = Instant.now();
    }

    public void iniciarIntento() {
        intentos++;
    }

    public void publicado(String shardId, String sequenceNumber) {
        this.shardId = shardId;
        this.sequenceNumber = sequenceNumber;
        this.publicadoEn = Instant.now();
        this.ultimoError = null;
    }

    public void registrarError(Exception error) {
        String mensaje = error.getMessage() == null
                ? error.getClass().getSimpleName()
                : error.getMessage();

        this.ultimoError = mensaje.substring(
                0, Math.min(mensaje.length(), 500));
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public String getPayload() {
        return payload;
    }

    public int getIntentos() {
        return intentos;
    }

    public Instant getPublicadoEn() {
        return publicadoEn;
    }

    public String getShardId() {
        return shardId;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public String getUltimoError() {
        return ultimoError;
    }
}