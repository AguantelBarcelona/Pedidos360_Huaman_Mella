package cl.pedidos360.pedidos_service.kinesis;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicadorKinesisTest {

    private final KinesisClient client =
            mock(KinesisClient.class);

    private final KinesisOutboxRepository repository =
            mock(KinesisOutboxRepository.class);

    private final String stream =
            "pedidos360-pedidos-stream";

    private final PublicadorKinesis publicador =
            new PublicadorKinesis(client, repository, stream);

    private final KinesisOutboxEvent evento =
            new KinesisOutboxEvent(
                    "evento-test-001",
                    "EVENTO_PRUEBA",
                    "42",
                    "{\"eventId\":\"evento-test-001\"}"
            );

    private PutRecordResponse respuestaValida() {

        PutRecordResponse.Builder builder =
                PutRecordResponse.builder()
                        .shardId("shardId-000000000001")
                        .sequenceNumber("12345");

        builder.sdkHttpResponse(
                SdkHttpResponse.builder()
                        .statusCode(200)
                        .build()
        );

        return builder.build();
    }

    @Test
    void publicarDebeConfirmarStreamParticionYRespuesta() {

        when(repository.pendientes(any()))
                .thenReturn(List.of(evento));

        when(client.putRecord(any(PutRecordRequest.class)))
                .thenReturn(respuestaValida());

        publicador.publicarPendientes();

        ArgumentCaptor<PutRecordRequest> captor =
                ArgumentCaptor.forClass(PutRecordRequest.class);

        verify(client).putRecord(captor.capture());

        PutRecordRequest solicitud = captor.getValue();

        assertEquals(stream, solicitud.streamName());
        assertEquals("42", solicitud.partitionKey());
        assertEquals(
                evento.getPayload(),
                solicitud.data().asUtf8String()
        );

        assertNotNull(evento.getPublicadoEn());
        assertEquals(
                "shardId-000000000001",
                evento.getShardId()
        );
        assertEquals("12345", evento.getSequenceNumber());
        assertEquals(1, evento.getIntentos());
    }

    @Test
    void falloDebeMantenerEventoPendiente() {

        when(repository.pendientes(any()))
                .thenReturn(List.of(evento));

        when(client.putRecord(any(PutRecordRequest.class)))
                .thenThrow(
                        new IllegalStateException("Fallo simulado")
                );

        publicador.publicarPendientes();

        assertNull(evento.getPublicadoEn());
        assertEquals(1, evento.getIntentos());
        assertEquals("Fallo simulado", evento.getUltimoError());
    }

    @Test
    void reintentoDebeConservarEventoOriginal() {

        when(repository.pendientes(any()))
                .thenReturn(List.of(evento));

        when(client.putRecord(any(PutRecordRequest.class)))
                .thenThrow(
                        new IllegalStateException("Fallo temporal")
                )
                .thenReturn(respuestaValida());

        publicador.publicarPendientes();

        assertNull(evento.getPublicadoEn());

        publicador.publicarPendientes();

        assertNotNull(evento.getPublicadoEn());
        assertEquals("evento-test-001", evento.getEventId());
        assertEquals(2, evento.getIntentos());
        assertNull(evento.getUltimoError());

        verify(client, times(2))
                .putRecord(any(PutRecordRequest.class));
    }
}
