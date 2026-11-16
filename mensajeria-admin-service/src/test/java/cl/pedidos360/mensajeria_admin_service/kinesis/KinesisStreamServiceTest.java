package cl.pedidos360.mensajeria_admin_service.kinesis;

import cl.pedidos360.mensajeria_admin_service.comun.AccesoDenegadoRecursoException;
import cl.pedidos360.mensajeria_admin_service.comun.AdminProperties;
import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.ConfirmacionRequeridaException;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.services.kinesis.paginators.ListStreamsIterable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KinesisStreamServiceTest {

    @Mock
    private KinesisClient kinesis;

    @Mock
    private AuditoriaAdmin auditoria;

    @Mock
    private ListStreamsIterable paginas;

    private KinesisStreamService servicio;

    @BeforeEach
    void setUp() {

        NombresRecursos nombres = new NombresRecursos(
                new AdminProperties(
                        "us-east-1",
                        "pedidos360",
                        true,
                        List.of()
                )
        );

        servicio = new KinesisStreamService(
                kinesis,
                nombres,
                auditoria,
                "pedidos360-pedidos-stream,pedidos360-inventario-stream,pedidos360-eventos-fallidos",
                6,
                168
        );
    }

    @Test
    void listarDebeFiltrarRecursosAjenos() {

        when(kinesis.listStreamsPaginator(
                any(ListStreamsRequest.class)))
                .thenReturn(paginas);

        when(paginas.iterator()).thenReturn(
                List.of(
                        ListStreamsResponse.builder()
                                .streamNames(
                                        "pedidos360-prueba",
                                        "otro-stream"
                                )
                                .build()
                ).iterator()
        );

        assertEquals(
                List.of("pedidos360-prueba"),
                servicio.listar()
        );
    }

    @Test
    void crearDebeUsarNombreNormalizadoYModoProvisionado() {

        var resultado = servicio.crear("prueba-ev3", 3);

        ArgumentCaptor<CreateStreamRequest> captor =
                ArgumentCaptor.forClass(CreateStreamRequest.class);

        verify(kinesis).createStream(captor.capture());

        CreateStreamRequest request = captor.getValue();

        assertEquals(
                "pedidos360-prueba-ev3",
                request.streamName()
        );

        assertEquals(
                3,
                request.shardCount().intValue()
        );

        assertEquals(
                StreamMode.PROVISIONED,
                request.streamModeDetails().streamMode()
        );

        assertEquals("SOLICITADO", resultado.estado());

        verify(auditoria).registrar(
                "KINESIS_CREAR_STREAM",
                "pedidos360-prueba-ev3",
                "shards=3"
        );
    }

    @Test
    void crearDebeRechazarCantidadInvalidaDeShards() {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crear("prueba-ev3", 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crear("prueba-ev3", 7)
        );

        verifyNoInteractions(kinesis);
    }

    @Test
    void actualizarDebeEnviarCantidadObjetivoDeShards() {

        simularStream(1, 24);

        servicio.actualizarShards("prueba-ev3", 2);

        ArgumentCaptor<UpdateShardCountRequest> captor =
                ArgumentCaptor.forClass(
                        UpdateShardCountRequest.class
                );

        verify(kinesis).updateShardCount(captor.capture());

        assertEquals(
                2,
                captor.getValue().targetShardCount().intValue()
        );

        assertEquals(
                ScalingType.UNIFORM_SCALING,
                captor.getValue().scalingType()
        );
    }

    @Test
    void actualizarRetencionDebeAumentarLasHoras() {

        simularStream(1, 24);

        servicio.actualizarRetencion(
                "prueba-ev3",
                48,
                null
        );

        ArgumentCaptor<IncreaseStreamRetentionPeriodRequest> captor =
                ArgumentCaptor.forClass(
                        IncreaseStreamRetentionPeriodRequest.class
                );

        verify(kinesis)
                .increaseStreamRetentionPeriod(captor.capture());

        assertEquals(
                48,
                captor.getValue().retentionPeriodHours().intValue()
        );
    }

    @Test
    void reducirRetencionDebeExigirConfirmacion() {

        simularStream(1, 48);

        assertThrows(
                ConfirmacionRequeridaException.class,
                () -> servicio.actualizarRetencion(
                        "prueba-ev3",
                        24,
                        null
                )
        );

        verify(kinesis, never())
                .decreaseStreamRetentionPeriod(
                        any(DecreaseStreamRetentionPeriodRequest.class)
                );
    }

    @Test
    void eliminarDebeExigirConfirmacionExacta() {

        assertThrows(
                ConfirmacionRequeridaException.class,
                () -> servicio.eliminar("prueba-ev3", null)
        );

        verifyNoInteractions(kinesis);
    }

    @Test
    void eliminarStreamProtegidoDebeRechazarse() {

        assertThrows(
                AccesoDenegadoRecursoException.class,
                () -> servicio.eliminar(
                        "pedidos360-pedidos-stream",
                        "pedidos360-pedidos-stream"
                )
        );

        verifyNoInteractions(kinesis);
    }

    @Test
    void eliminarStreamDePruebaConfirmadoDebeSolicitarEliminacion() {

        var resultado = servicio.eliminar(
                "prueba-ev3",
                "pedidos360-prueba-ev3"
        );

        ArgumentCaptor<DeleteStreamRequest> captor =
                ArgumentCaptor.forClass(
                        DeleteStreamRequest.class
                );

        verify(kinesis).deleteStream(captor.capture());

        assertEquals(
                "pedidos360-prueba-ev3",
                captor.getValue().streamName()
        );

        assertEquals("SOLICITADO", resultado.estado());
    }

    private void simularStream(int shards, int retencion) {

        when(kinesis.describeStreamSummary(
                any(DescribeStreamSummaryRequest.class)))
                .thenReturn(
                        DescribeStreamSummaryResponse.builder()
                                .streamDescriptionSummary(
                                        StreamDescriptionSummary.builder()
                                                .streamName("pedidos360-prueba-ev3")
                                                .openShardCount(shards)
                                                .retentionPeriodHours(retencion)
                                                .streamStatus(StreamStatus.ACTIVE)
                                                .build()
                                )
                                .build()
                );
    }
}
