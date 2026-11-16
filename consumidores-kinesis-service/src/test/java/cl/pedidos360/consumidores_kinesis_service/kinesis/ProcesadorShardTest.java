package cl.pedidos360.consumidores_kinesis_service.kinesis;

import cl.pedidos360.consumidores_kinesis_service.procesamiento.DestinoConsumidor;
import cl.pedidos360.consumidores_kinesis_service.procesamiento.ProcesadorEventos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcesadorShardTest {

    private ProcesadorEventos procesador;
    private PublicadorFallidos publicador;
    private RecordProcessorCheckpointer checkpointer;
    private Runnable detenerWorker;
    private ProcesadorShard consumidor;

    private static final String APP = "p360-pedidos-analitica";

    private static final String EVENT_ID =
            "4f2b20f4-e50e-46d2-b298-8f0ecbc811e0";

    private static final String PAYLOAD =
            "{\"eventId\":\"" + EVENT_ID + "\"}";

    @BeforeEach
    void setUp() {

        procesador = mock(ProcesadorEventos.class);
        publicador = mock(PublicadorFallidos.class);
        checkpointer = mock(RecordProcessorCheckpointer.class);
        detenerWorker = mock(Runnable.class);

        consumidor = new ProcesadorShard(
                APP,
                "pedidos360-pedidos-stream",
                "worker-test",
                DestinoConsumidor.PEDIDOS_ANALITICA,
                procesador,
                publicador,
                3,
                0,
                detenerWorker
        );

        consumidor.initialize(
                InitializationInput.builder()
                        .shardId("shardId-000000000001")
                        .build()
        );
    }

    private KinesisClientRecord registro(
            String secuencia,
            String payload) {

        return KinesisClientRecord.builder()
                .sequenceNumber(secuencia)
                .subSequenceNumber(0L)
                .partitionKey("37")
                .data(ByteBuffer.wrap(
                        payload.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    private ProcessRecordsInput entrada(
            KinesisClientRecord... registros) {

        return ProcessRecordsInput.builder()
                .records(List.of(registros))
                .checkpointer(checkpointer)
                .millisBehindLatest(0L)
                .build();
    }

    @Test
    void debeGuardarCheckpointDespuesDeProcesar() throws Exception {

        when(procesador.procesar(
                APP,
                DestinoConsumidor.PEDIDOS_ANALITICA,
                "37",
                PAYLOAD
        )).thenReturn(new ProcesadorEventos.Resultado(
                EVENT_ID, true));

        consumidor.processRecords(
                entrada(registro("100", PAYLOAD)));

        var orden = inOrder(procesador, checkpointer);

        orden.verify(procesador).procesar(
                APP,
                DestinoConsumidor.PEDIDOS_ANALITICA,
                "37",
                PAYLOAD
        );

        orden.verify(checkpointer).checkpoint("100", 0L);

        verifyNoInteractions(publicador, detenerWorker);
    }

    @Test
    void debeGuardarCheckpointDeUnDuplicado() throws Exception {

        when(procesador.procesar(
                anyString(), any(), anyString(), anyString()
        )).thenReturn(new ProcesadorEventos.Resultado(
                EVENT_ID, false));

        consumidor.processRecords(
                entrada(registro("100", PAYLOAD)));

        verify(checkpointer).checkpoint("100", 0L);
        verifyNoInteractions(publicador, detenerWorker);
    }

    @Test
    void debeReintentarUnFalloTemporal() throws Exception {

        when(procesador.procesar(
                anyString(), any(), anyString(), anyString()
        )).thenThrow(new IllegalStateException("Fallo temporal"))
          .thenReturn(new ProcesadorEventos.Resultado(
                  EVENT_ID, true));

        consumidor.processRecords(
                entrada(registro("100", PAYLOAD)));

        verify(procesador, times(2)).procesar(
                anyString(), any(), anyString(), anyString()
        );

        verify(checkpointer).checkpoint("100", 0L);
        verifyNoInteractions(publicador, detenerWorker);
    }

    @Test
    void debeEnviarEventoFallidoYContinuar() throws Exception {

        String invalido = "{json-invalido}";

        when(procesador.procesar(
                APP,
                DestinoConsumidor.PEDIDOS_ANALITICA,
                "37",
                invalido
        )).thenThrow(new IllegalArgumentException("JSON invalido"));

        when(procesador.procesar(
                APP,
                DestinoConsumidor.PEDIDOS_ANALITICA,
                "37",
                PAYLOAD
        )).thenReturn(new ProcesadorEventos.Resultado(
                EVENT_ID, true));

        consumidor.processRecords(entrada(
                registro("100", invalido),
                registro("101", PAYLOAD)
        ));

        verify(procesador, times(3)).procesar(
                APP,
                DestinoConsumidor.PEDIDOS_ANALITICA,
                "37",
                invalido
        );

        verify(publicador).publicar(argThat(evento ->
                evento.intentos() == 3
                        && evento.aplicacion().equals(APP)
                        && evento.sequenceNumber().equals("100")
        ));

        verify(checkpointer).checkpoint("100", 0L);
        verify(checkpointer).checkpoint("101", 0L);

        verifyNoInteractions(detenerWorker);
    }

    @Test
    void noDebeAvanzarSiFallaElStreamDeErrores() throws Exception {

        when(procesador.procesar(
                anyString(), any(), anyString(), anyString()
        )).thenThrow(new IllegalArgumentException("Evento invalido"));

        doThrow(new IllegalStateException("Kinesis no disponible"))
                .when(publicador).publicar(any());

        consumidor.processRecords(entrada(
                registro("100", PAYLOAD),
                registro("101", PAYLOAD)
        ));

        verify(publicador, times(3)).publicar(any());

        verify(procesador, times(3)).procesar(
                anyString(), any(), anyString(), anyString()
        );

        verifyNoInteractions(checkpointer);

        verify(detenerWorker).run();

        consumidor.processRecords(
                entrada(registro("102", PAYLOAD)));

        verifyNoInteractions(checkpointer);
        verifyNoMoreInteractions(detenerWorker);
    }
}
