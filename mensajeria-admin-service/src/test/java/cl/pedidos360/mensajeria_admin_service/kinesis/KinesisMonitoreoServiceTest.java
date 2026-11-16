package cl.pedidos360.mensajeria_admin_service.kinesis;

import cl.pedidos360.mensajeria_admin_service.comun.AccesoDenegadoRecursoException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KinesisMonitoreoServiceTest {

    @Mock
    private DynamoDbClient dynamodb;

    @Mock
    private CloudWatchClient cloudwatch;

    @Mock
    private ScanIterable paginas;

    private KinesisMonitoreoService servicio;

    @BeforeEach
    void setUp() {

        servicio = new KinesisMonitoreoService(
                dynamodb,
                cloudwatch,
                "p360-pedidos-analitica",
                "p360-pedidos-trazabilidad",
                "p360-inventario-proyeccion",
                "pedidos360-pedidos-stream",
                "pedidos360-inventario-stream"
        );
    }

    @Test
    void listarDebeRetornarTresAplicaciones() {

        assertEquals(3, servicio.listar().size());

        verifyNoInteractions(dynamodb, cloudwatch);
    }

    @Test
    void checkpointsDebeAgruparLeasesYWorkers() {

        Map<String, AttributeValue> primero = Map.of(
                "leaseKey", texto("shardId-000000000000"),
                "leaseOwner", texto("worker-1"),
                "checkpoint", texto("TRIM_HORIZON")
        );

        Map<String, AttributeValue> segundo = Map.of(
                "leaseKey", texto("shardId-000000000001"),
                "leaseOwner", texto("worker-2"),
                "checkpoint", texto("123456789")
        );

        when(dynamodb.scanPaginator(any(ScanRequest.class)))
                .thenReturn(paginas);

        when(paginas.iterator()).thenReturn(
                List.of(
                        ScanResponse.builder()
                                .items(primero, segundo)
                                .build()
                ).iterator()
        );

        var resultado = servicio.checkpoints(
                "p360-pedidos-analitica"
        );

        assertEquals(2, resultado.totalLeases());
        assertEquals(2L, resultado.workersAsignados());

        assertEquals(
                "123456789",
                resultado.checkpoints().get(1).checkpoint()
        );
    }

    @Test
    void checkpointsDebeRechazarTablasNoAutorizadas() {

        assertThrows(
                AccesoDenegadoRecursoException.class,
                () -> servicio.checkpoints("tabla-externa")
        );

        verifyNoInteractions(dynamodb);
    }

    @Test
    void lagDebeConsultarMetricaDeAplicacion() {

        Instant fecha = Instant.parse(
                "2026-11-16T19:15:00Z"
        );

        when(cloudwatch.getMetricStatistics(
                any(GetMetricStatisticsRequest.class)))
                .thenReturn(
                        GetMetricStatisticsResponse.builder()
                                .datapoints(
                                        Datapoint.builder()
                                                .timestamp(fecha)
                                                .maximum(0.0)
                                                .build()
                                )
                                .build()
                );

        var resultado = servicio.lag(
                "p360-pedidos-analitica",
                null
        );

        assertEquals("DISPONIBLE", resultado.estado());
        assertEquals(0.0, resultado.lagMs());
        assertEquals(fecha, resultado.fecha());

        ArgumentCaptor<GetMetricStatisticsRequest> captor =
                ArgumentCaptor.forClass(
                        GetMetricStatisticsRequest.class
                );

        verify(cloudwatch)
                .getMetricStatistics(captor.capture());

        assertEquals(
                "MillisBehindLatest",
                captor.getValue().metricName()
        );

        assertEquals(
                "ApplicationTracker",
                captor.getValue().dimensions().get(0).value()
        );
    }

    @Test
    void lagSinMetricasNoDebeInformarCeroFalsamente() {

        when(cloudwatch.getMetricStatistics(
                any(GetMetricStatisticsRequest.class)))
                .thenReturn(
                        GetMetricStatisticsResponse.builder()
                                .datapoints(List.of())
                                .build()
                );

        var resultado = servicio.lag(
                "p360-pedidos-trazabilidad",
                null
        );

        assertEquals("SIN_DATOS", resultado.estado());
        assertNull(resultado.lagMs());
    }

    @Test
    void lagDebeRechazarShardInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.lag(
                        "p360-pedidos-analitica",
                        "shard-no-valido"
                )
        );

        verifyNoInteractions(cloudwatch);
    }

    private AttributeValue texto(String valor) {

        return AttributeValue.builder()
                .s(valor)
                .build();
    }
}
