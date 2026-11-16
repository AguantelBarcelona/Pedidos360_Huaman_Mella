package cl.pedidos360.mensajeria_admin_service.kinesis;

import cl.pedidos360.mensajeria_admin_service.comun.AccesoDenegadoRecursoException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class KinesisMonitoreoService {

    private final DynamoDbClient dynamodb;
    private final CloudWatchClient cloudwatch;

    private final List<Aplicacion> aplicaciones;

    public KinesisMonitoreoService(
            DynamoDbClient dynamodb,
            CloudWatchClient cloudwatch,

            @Value("${app.admin.kinesis.analitica-app:p360-pedidos-analitica}")
            String analitica,

            @Value("${app.admin.kinesis.trazabilidad-app:p360-pedidos-trazabilidad}")
            String trazabilidad,

            @Value("${app.admin.kinesis.inventario-app:p360-inventario-proyeccion}")
            String inventario,

            @Value("${app.admin.kinesis.pedidos-stream:pedidos360-pedidos-stream}")
            String pedidosStream,

            @Value("${app.admin.kinesis.inventario-stream:pedidos360-inventario-stream}")
            String inventarioStream) {

        this.dynamodb = dynamodb;
        this.cloudwatch = cloudwatch;

        this.aplicaciones = List.of(
                new Aplicacion(
                        analitica,
                        pedidosStream,
                        analitica
                ),
                new Aplicacion(
                        trazabilidad,
                        pedidosStream,
                        trazabilidad
                ),
                new Aplicacion(
                        inventario,
                        inventarioStream,
                        inventario
                )
        );

        if (aplicaciones.stream()
                .map(Aplicacion::nombre)
                .distinct()
                .count() != 3) {

            throw new IllegalArgumentException(
                    "Las aplicaciones KCL deben tener nombres distintos"
            );
        }
    }

    public record Aplicacion(
            String nombre,
            String stream,
            String tablaLeases
    ) {}

    public record Checkpoint(
            String shard,
            String worker,
            String checkpoint,
            String subsecuencia,
            String leaseCounter
    ) {}

    public record CheckpointsInfo(
            String aplicacion,
            String stream,
            int totalLeases,
            long workersAsignados,
            List<Checkpoint> checkpoints
    ) {}

    public record PuntoLag(
            Instant fecha,
            Double lagMs
    ) {}

    public record LagInfo(
            String aplicacion,
            String stream,
            String shard,
            String metrica,
            String estado,
            Double lagMs,
            Instant fecha,
            List<PuntoLag> muestras
    ) {}

    public List<Aplicacion> listar() {
        return aplicaciones;
    }

    public CheckpointsInfo checkpoints(String nombre) {

        Aplicacion app = buscar(nombre);

        List<Checkpoint> resultado = new ArrayList<>();

        ScanRequest request = ScanRequest.builder()
                .tableName(app.tablaLeases())
                .projectionExpression(
                        "leaseKey, leaseOwner, #cp, checkpointSubSequenceNumber, leaseCounter"
                )
                .expressionAttributeNames(
                        Map.of("#cp", "checkpoint")
                )
                .build();

        for (ScanResponse pagina :
                dynamodb.scanPaginator(request)) {

            for (Map<String, AttributeValue> item :
                    pagina.items()) {

                String shard = texto(item, "leaseKey");

                if (shard == null
                        || !shard.startsWith("shardId-")) {

                    continue;
                }

                resultado.add(
                        new Checkpoint(
                                shard,
                                texto(item, "leaseOwner"),
                                texto(item, "checkpoint"),
                                numero(item, "checkpointSubSequenceNumber"),
                                numero(item, "leaseCounter")
                        )
                );
            }
        }

        resultado.sort(
                Comparator.comparing(Checkpoint::shard)
        );

        long workers = resultado.stream()
                .map(Checkpoint::worker)
                .filter(w -> w != null && !w.isBlank())
                .distinct()
                .count();

        return new CheckpointsInfo(
                app.nombre(),
                app.stream(),
                resultado.size(),
                workers,
                List.copyOf(resultado)
        );
    }

    public LagInfo lag(String nombre, String shard) {

        Aplicacion app = buscar(nombre);

        List<Dimension> dimensiones = new ArrayList<>();

        if (shard == null || shard.isBlank()) {

            shard = null;

            dimensiones.add(
                    Dimension.builder()
                            .name("Operation")
                            .value("ApplicationTracker")
                            .build()
            );

        } else {

            if (!shard.matches("shardId-[0-9]{12}")) {

                throw new IllegalArgumentException(
                        "Identificador de shard invalido"
                );
            }

            dimensiones.add(
                    Dimension.builder()
                            .name("Operation")
                            .value("ProcessTask")
                            .build()
            );

            dimensiones.add(
                    Dimension.builder()
                            .name("ShardId")
                            .value(shard)
                            .build()
            );
        }

        Instant ahora = Instant.now();

        List<PuntoLag> puntos = cloudwatch.getMetricStatistics(
                GetMetricStatisticsRequest.builder()
                        .namespace(app.nombre())
                        .metricName("MillisBehindLatest")
                        .dimensions(dimensiones)
                        .startTime(
                                ahora.minus(10, ChronoUnit.MINUTES)
                        )
                        .endTime(ahora)
                        .period(60)
                        .statistics(Statistic.MAXIMUM)
                        .build()
        ).datapoints().stream()

                .filter(p -> p.timestamp() != null
                        && p.maximum() != null)

                .sorted(
                        Comparator.comparing(Datapoint::timestamp)
                )

                .map(p -> new PuntoLag(
                        p.timestamp(),
                        p.maximum()
                ))

                .toList();

        if (puntos.isEmpty()) {

            return new LagInfo(
                    app.nombre(),
                    app.stream(),
                    shard,
                    "MillisBehindLatest",
                    "SIN_DATOS",
                    null,
                    null,
                    List.of()
            );
        }

        PuntoLag ultimo = puntos.get(puntos.size() - 1);

        return new LagInfo(
                app.nombre(),
                app.stream(),
                shard,
                "MillisBehindLatest",
                "DISPONIBLE",
                ultimo.lagMs(),
                ultimo.fecha(),
                puntos
        );
    }

    private Aplicacion buscar(String nombre) {

        return aplicaciones.stream()
                .filter(a -> a.nombre().equals(nombre))
                .findFirst()
                .orElseThrow(
                        () -> new AccesoDenegadoRecursoException(
                                "Aplicacion KCL fuera del alcance administrativo"
                        )
                );
    }

    private String texto(
            Map<String, AttributeValue> item,
            String clave) {

        AttributeValue valor = item.get(clave);

        return valor == null ? null : valor.s();
    }

    private String numero(
            Map<String, AttributeValue> item,
            String clave) {

        AttributeValue valor = item.get(clave);

        return valor == null ? null : valor.n();
    }
}
