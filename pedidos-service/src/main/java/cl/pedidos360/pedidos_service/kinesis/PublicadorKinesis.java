package cl.pedidos360.pedidos_service.kinesis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

@Component
@ConditionalOnProperty(
        name = "app.kinesis.enabled",
        havingValue = "true"
)
public class PublicadorKinesis {

    private static final Logger log =
            LoggerFactory.getLogger(PublicadorKinesis.class);

    private final KinesisClient client;
    private final KinesisOutboxRepository repository;
    private final String stream;

    public PublicadorKinesis(
            KinesisClient client,
            KinesisOutboxRepository repository,
            @Value("${app.kinesis.stream-name}") String stream) {

        this.client = client;
        this.repository = repository;
        this.stream = stream;
    }

    @Scheduled(fixedDelayString = "${app.kinesis.poll-delay-ms:5000}")
    @Transactional
    public void publicarPendientes() {

        for (KinesisOutboxEvent evento :
                repository.pendientes(PageRequest.of(0, 10))) {

            evento.iniciarIntento();

            try {

                PutRecordResponse respuesta = client.putRecord(
                        PutRecordRequest.builder()
                                .streamName(stream)
                                .partitionKey(evento.getPartitionKey())
                                .data(SdkBytes.fromUtf8String(
                                        evento.getPayload()))
                                .build()
                );

                if (!respuesta.sdkHttpResponse().isSuccessful()
                        || respuesta.shardId() == null
                        || respuesta.shardId().isBlank()
                        || respuesta.sequenceNumber() == null
                        || respuesta.sequenceNumber().isBlank()) {

                    throw new IllegalStateException(
                            "Kinesis no confirmó la publicación");
                }

                evento.publicado(
                        respuesta.shardId(),
                        respuesta.sequenceNumber()
                );

                log.info(
                        "KINESIS_PUBLICADO eventId={} tipo={} stream={} partitionKey={} shardId={} sequenceNumber={} intento={}",
                        evento.getEventId(),
                        evento.getEventType(),
                        stream,
                        evento.getPartitionKey(),
                        respuesta.shardId(),
                        respuesta.sequenceNumber(),
                        evento.getIntentos()
                );

            } catch (Exception error) {

                evento.registrarError(error);

                log.error(
                        "KINESIS_ERROR eventId={} stream={} intento={} causa={}",
                        evento.getEventId(),
                        stream,
                        evento.getIntentos(),
                        evento.getUltimoError()
                );

                // Se reintentará en el siguiente ciclo.
                break;
            }
        }
    }
}