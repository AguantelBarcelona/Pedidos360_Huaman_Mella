package cl.pedidos360.consumidores_kinesis_service.kinesis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

public class PublicadorFallidos {

    private static final Logger log =
            LoggerFactory.getLogger(PublicadorFallidos.class);

    private final KinesisAsyncClient cliente;
    private final String stream;
    private final long timeoutMs;

    private final JsonMapper mapper = JsonMapper.builder().build();

    public PublicadorFallidos(
            KinesisAsyncClient cliente,
            String stream,
            long timeoutMs) {

        this.cliente = cliente;
        this.stream = stream;
        this.timeoutMs = timeoutMs;
    }

    public record EventoFallido(
            String failureId,
            String eventId,
            String aplicacion,
            String streamOrigen,
            String shardId,
            String sequenceNumber,
            long subSequenceNumber,
            String partitionKey,
            int intentos,
            String causa,
            String payloadBase64,
            String fechaError
    ) {}

    public void publicar(EventoFallido evento) throws Exception {

        PutRecordRequest request = PutRecordRequest.builder()
                .streamName(stream)
                .partitionKey(evento.failureId())
                .data(SdkBytes.fromByteArray(
                        mapper.writeValueAsBytes(evento)))
                .build();

        PutRecordResponse response = cliente.putRecord(request)
                .get(timeoutMs, TimeUnit.MILLISECONDS);

        if (!response.sdkHttpResponse().isSuccessful()
                || response.shardId() == null
                || response.sequenceNumber() == null) {

            throw new IllegalStateException(
                    "Kinesis no confirmo el evento fallido");
        }

        log.info(
                "KINESIS_FALLIDO_PUBLICADO eventId={} failureId={} app={} stream={} shardId={} sequenceNumber={}",
                evento.eventId(),
                evento.failureId(),
                evento.aplicacion(),
                stream,
                response.shardId(),
                response.sequenceNumber()
        );
    }
}
