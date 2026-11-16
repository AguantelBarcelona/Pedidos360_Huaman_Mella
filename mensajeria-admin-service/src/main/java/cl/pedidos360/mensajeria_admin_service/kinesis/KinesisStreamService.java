package cl.pedidos360.mensajeria_admin_service.kinesis;

import cl.pedidos360.mensajeria_admin_service.comun.AccesoDenegadoRecursoException;
import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class KinesisStreamService {

    private final KinesisClient kinesis;
    private final NombresRecursos nombres;
    private final AuditoriaAdmin auditoria;

    private final Set<String> protegidos;
    private final int maxShards;
    private final int maxRetencionHoras;

    public KinesisStreamService(
            KinesisClient kinesis,
            NombresRecursos nombres,
            AuditoriaAdmin auditoria,

            @Value("${app.admin.kinesis.protected-streams:pedidos360-pedidos-stream,pedidos360-inventario-stream,pedidos360-eventos-fallidos}")
            String streamsProtegidos,

            @Value("${app.admin.kinesis.max-shards:6}")
            int maxShards,

            @Value("${app.admin.kinesis.max-retention-hours:168}")
            int maxRetencionHoras) {

        this.kinesis = kinesis;
        this.nombres = nombres;
        this.auditoria = auditoria;

        this.protegidos = Arrays.stream(streamsProtegidos.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());

        this.maxShards = maxShards;
        this.maxRetencionHoras = maxRetencionHoras;
    }

    public record StreamInfo(
            String nombre,
            String arn,
            String estado,
            String modo,
            Integer shards,
            Integer retencionHoras,
            Instant fechaCreacion,
            boolean protegido
    ) {}

    public record Operacion(
            String stream,
            String operacion,
            String estado,
            Integer valor
    ) {}

    public List<String> listar() {

        Set<String> resultado = new TreeSet<>();

        for (ListStreamsResponse pagina :
                kinesis.listStreamsPaginator(
                        ListStreamsRequest.builder().build())) {

            pagina.streamNames().stream()
                    .filter(n -> n.startsWith(nombres.prefijo() + "-"))
                    .forEach(resultado::add);
        }

        return List.copyOf(resultado);
    }

    public StreamInfo detalle(String nombre) {

        String completo = nombres.normalizar(nombre);

        StreamDescriptionSummary stream = consultar(completo);

        String modo = stream.streamModeDetails() == null
                ? "DESCONOCIDO"
                : stream.streamModeDetails().streamModeAsString();

        return new StreamInfo(
                stream.streamName(),
                stream.streamARN(),
                stream.streamStatusAsString(),
                modo,
                stream.openShardCount(),
                stream.retentionPeriodHours(),
                stream.streamCreationTimestamp(),
                protegidos.contains(completo)
        );
    }

    public Operacion crear(String nombre, Integer shards) {

        String completo = nombres.normalizar(nombre);

        validarModificacion(completo);
        validarShards(shards);

        kinesis.createStream(
                CreateStreamRequest.builder()
                        .streamName(completo)
                        .shardCount(shards)
                        .streamModeDetails(
                                StreamModeDetails.builder()
                                        .streamMode(StreamMode.PROVISIONED)
                                        .build()
                        )
                        .build()
        );

        auditoria.registrar(
                "KINESIS_CREAR_STREAM",
                completo,
                "shards=" + shards
        );

        return new Operacion(
                completo,
                "CREAR",
                "SOLICITADO",
                shards
        );
    }

    public Operacion actualizarShards(
            String nombre,
            Integer shards) {

        String completo = nombres.normalizar(nombre);

        validarModificacion(completo);
        validarShards(shards);

        StreamDescriptionSummary actual = consultar(completo);

        if (actual.openShardCount().equals(shards)) {
            return new Operacion(
                    completo,
                    "ACTUALIZAR_SHARDS",
                    "SIN_CAMBIOS",
                    shards
            );
        }

        kinesis.updateShardCount(
                UpdateShardCountRequest.builder()
                        .streamName(completo)
                        .targetShardCount(shards)
                        .scalingType(ScalingType.UNIFORM_SCALING)
                        .build()
        );

        auditoria.registrar(
                "KINESIS_ACTUALIZAR_SHARDS",
                completo,
                "shards=" + shards
        );

        return new Operacion(
                completo,
                "ACTUALIZAR_SHARDS",
                "SOLICITADO",
                shards
        );
    }

    public Operacion actualizarRetencion(
            String nombre,
            Integer horas,
            String confirmar) {

        String completo = nombres.normalizar(nombre);

        validarModificacion(completo);

        if (horas == null
                || horas < 24
                || horas > maxRetencionHoras) {

            throw new IllegalArgumentException(
                    "La retencion debe estar entre 24 y "
                            + maxRetencionHoras + " horas"
            );
        }

        StreamDescriptionSummary actual = consultar(completo);

        int anterior = actual.retentionPeriodHours();

        if (horas > anterior) {

            kinesis.increaseStreamRetentionPeriod(
                    IncreaseStreamRetentionPeriodRequest.builder()
                            .streamName(completo)
                            .retentionPeriodHours(horas)
                            .build()
            );

        } else if (horas < anterior) {

            nombres.exigirConfirmacion(completo, confirmar);

            kinesis.decreaseStreamRetentionPeriod(
                    DecreaseStreamRetentionPeriodRequest.builder()
                            .streamName(completo)
                            .retentionPeriodHours(horas)
                            .build()
            );

        } else {

            return new Operacion(
                    completo,
                    "ACTUALIZAR_RETENCION",
                    "SIN_CAMBIOS",
                    horas
            );
        }

        auditoria.registrar(
                "KINESIS_ACTUALIZAR_RETENCION",
                completo,
                "horas=" + horas
        );

        return new Operacion(
                completo,
                "ACTUALIZAR_RETENCION",
                "SOLICITADO",
                horas
        );
    }

    public Operacion eliminar(
            String nombre,
            String confirmar) {

        String completo = nombres.normalizar(nombre);

        validarModificacion(completo);

        nombres.exigirConfirmacion(completo, confirmar);

        kinesis.deleteStream(
                DeleteStreamRequest.builder()
                        .streamName(completo)
                        .enforceConsumerDeletion(false)
                        .build()
        );

        auditoria.registrar(
                "KINESIS_ELIMINAR_STREAM",
                completo,
                "confirmado"
        );

        return new Operacion(
                completo,
                "ELIMINAR",
                "SOLICITADO",
                null
        );
    }

    private StreamDescriptionSummary consultar(String completo) {

        return kinesis.describeStreamSummary(
                DescribeStreamSummaryRequest.builder()
                        .streamName(completo)
                        .build()
        ).streamDescriptionSummary();
    }

    private void validarShards(Integer shards) {

        if (shards == null || shards < 1 || shards > maxShards) {

            throw new IllegalArgumentException(
                    "La cantidad de shards debe estar entre 1 y "
                            + maxShards
            );
        }
    }

    private void validarModificacion(String completo) {

        if (protegidos.contains(completo)) {

            throw new AccesoDenegadoRecursoException(
                    "El stream esta protegido y debe administrarse mediante CloudFormation"
            );
        }
    }
}
