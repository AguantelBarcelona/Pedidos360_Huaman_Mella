package cl.pedidos360.consumidores_kinesis_service.kinesis;

import cl.pedidos360.consumidores_kinesis_service.procesamiento.DestinoConsumidor;
import cl.pedidos360.consumidores_kinesis_service.procesamiento.ProcesadorEventos;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.metrics.MetricsLevel;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(
        name = "app.kinesis.enabled",
        havingValue = "true"
)
public class KinesisSchedulerManager implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(KinesisSchedulerManager.class);

    private final Environment env;
    private final ProcesadorEventos procesador;

    private final List<Scheduler> schedulers = new ArrayList<>();

    private final AtomicBoolean cerrando = new AtomicBoolean(false);
    private final AtomicBoolean falloFatal = new AtomicBoolean(false);

    private KinesisAsyncClient kinesis;
    private DynamoDbAsyncClient dynamodb;
    private CloudWatchAsyncClient cloudwatch;
    private PublicadorFallidos publicadorFallidos;

    private String workerId;

    private int maxIntentos;
    private int maxRecords;
    private long retryDelayMs;
    private long pollMs;

    private MetricsLevel metricsLevel;
    private InitialPositionInStreamExtended posicionInicial;

    public KinesisSchedulerManager(
            Environment env,
            ProcesadorEventos procesador) {

        this.env = env;
        this.procesador = procesador;
    }

    @Override
    public void run(ApplicationArguments args) {

        String pedidosStream = propiedad("pedidos-stream");
        String inventarioStream = propiedad("inventario-stream");
        String fallidosStream = propiedad("fallidos-stream");

        String analiticaApp = propiedad("analitica-app");
        String trazabilidadApp = propiedad("trazabilidad-app");
        String inventarioApp = propiedad("inventario-app");

        if (new HashSet<>(List.of(
                analiticaApp,
                trazabilidadApp,
                inventarioApp)).size() != 3) {

            throw new IllegalArgumentException(
                    "Las aplicaciones KCL deben tener nombres distintos");
        }

        if (new HashSet<>(List.of(
                pedidosStream,
                inventarioStream,
                fallidosStream)).size() != 3) {

            throw new IllegalArgumentException(
                    "Los streams deben tener nombres distintos");
        }

        maxIntentos = env.getRequiredProperty(
                "app.kinesis.max-attempts", Integer.class);

        retryDelayMs = env.getRequiredProperty(
                "app.kinesis.retry-delay-ms", Long.class);

        pollMs = env.getRequiredProperty(
                "app.kinesis.poll-ms", Long.class);

        maxRecords = env.getRequiredProperty(
                "app.kinesis.max-records", Integer.class);

        long timeoutMs = env.getRequiredProperty(
                "app.kinesis.timeout-ms", Long.class);

        if (maxIntentos < 1 || maxIntentos > 10
                || retryDelayMs < 0 || retryDelayMs > 30000
                || pollMs < 1000
                || maxRecords < 1 || maxRecords > 10000
                || timeoutMs <= 0) {

            throw new IllegalArgumentException(
                    "Parametros Kinesis invalidos");
        }

        InitialPositionInStream posicion =
                InitialPositionInStream.valueOf(
                        propiedad("initial-position")
                                .toUpperCase(Locale.ROOT));

        if (posicion == InitialPositionInStream.AT_TIMESTAMP) {
            throw new IllegalArgumentException(
                    "Use TRIM_HORIZON o LATEST");
        }

        posicionInicial =
                InitialPositionInStreamExtended
                        .newInitialPosition(posicion);

        metricsLevel = MetricsLevel.valueOf(
                propiedad("metrics-level")
                        .toUpperCase(Locale.ROOT));

        workerId = propiedad("worker-id")
                + "-" + UUID.randomUUID();

        Region region = Region.of(propiedad("aws-region"));
        Duration timeout = Duration.ofMillis(timeoutMs);

        kinesis = KinesisAsyncClient.builder()
                .region(region)
                .overrideConfiguration(c -> c.apiCallTimeout(timeout))
                .build();

        dynamodb = DynamoDbAsyncClient.builder()
                .region(region)
                .overrideConfiguration(c -> c.apiCallTimeout(timeout))
                .build();

        cloudwatch = CloudWatchAsyncClient.builder()
                .region(region)
                .overrideConfiguration(c -> c.apiCallTimeout(timeout))
                .build();

        publicadorFallidos = new PublicadorFallidos(
                kinesis,
                fallidosStream,
                timeoutMs
        );

        Scheduler analitica = crearScheduler(
                pedidosStream,
                analiticaApp,
                DestinoConsumidor.PEDIDOS_ANALITICA
        );

        Scheduler trazabilidad = crearScheduler(
                pedidosStream,
                trazabilidadApp,
                DestinoConsumidor.PEDIDOS_TRAZABILIDAD
        );

        Scheduler inventario = crearScheduler(
                inventarioStream,
                inventarioApp,
                DestinoConsumidor.INVENTARIO_PROYECCION
        );

        schedulers.add(analitica);
        schedulers.add(trazabilidad);
        schedulers.add(inventario);

        iniciarScheduler(analitica, analiticaApp);
        iniciarScheduler(trazabilidad, trazabilidadApp);
        iniciarScheduler(inventario, inventarioApp);

        log.info(
                "KCL_WORKER_INICIADO worker={} region={} aplicaciones=3",
                workerId,
                region.id()
        );
    }

    private Scheduler crearScheduler(
            String stream,
            String aplicacion,
            DestinoConsumidor destino) {

        ConfigsBuilder configs = new ConfigsBuilder(
                stream,
                aplicacion,
                kinesis,
                dynamodb,
                cloudwatch,
                workerId,
                () -> new ProcesadorShard(
                        aplicacion,
                        stream,
                        workerId,
                        destino,
                        procesador,
                        publicadorFallidos,
                        maxIntentos,
                        retryDelayMs,
                        this::solicitarReinicio
                )
        );

        var leases = configs.leaseManagementConfig()
                .maxLeaseRenewalThreads(4);

        var retrieval = configs.retrievalConfig()
                .retrievalSpecificConfig(
                        new PollingConfig(stream, kinesis)
                                .idleTimeBetweenReadsInMillis(pollMs)
                                .maxRecords(maxRecords)
                );

        retrieval.initialPositionInStreamExtended(posicionInicial);

        var metricas = configs.metricsConfig()
                .metricsLevel(metricsLevel)
                .metricsEnabledDimensions(
                        new HashSet<>(Set.of("Operation", "ShardId"))
                );

        log.info(
                "KCL_APLICACION_CONFIGURADA app={} stream={} worker={} modo=POLLING",
                aplicacion,
                stream,
                workerId
        );

        return new Scheduler(
                configs.checkpointConfig(),
                configs.coordinatorConfig(),
                leases,
                configs.lifecycleConfig(),
                metricas,
                configs.processorConfig(),
                retrieval
        );
    }

    private void iniciarScheduler(
            Scheduler scheduler,
            String aplicacion) {

        Thread hilo = new Thread(() -> {

            try {
                scheduler.run();

            } catch (Throwable ex) {

                log.error("KCL_SCHEDULER_ERROR app={}", aplicacion, ex);

            } finally {

                if (!cerrando.get()) {
                    solicitarReinicio();
                }
            }

        }, "kcl-" + aplicacion);

        hilo.setDaemon(false);
        hilo.start();
    }

    private String propiedad(String nombre) {

        String valor = env.getRequiredProperty(
                "app.kinesis." + nombre);

        if (valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Propiedad vacia: " + nombre);
        }

        return valor;
    }

    private void solicitarReinicio() {

        if (cerrando.get()) {
            return;
        }

        if (falloFatal.compareAndSet(false, true)) {

            log.error(
                    "KCL_REINICIO_REQUERIDO worker={}",
                    workerId
            );

            Thread hilo = new Thread(
                    () -> System.exit(1),
                    "kcl-reinicio-seguro"
            );

            hilo.setDaemon(false);
            hilo.start();
        }
    }

    @PreDestroy
    public void detener() {

        cerrando.set(true);

        List<Future<Boolean>> cierres = new ArrayList<>();

        for (Scheduler scheduler : schedulers) {

            try {
                cierres.add(scheduler.startGracefulShutdown());

            } catch (Exception ex) {
                log.warn("Error solicitando cierre KCL", ex);
            }
        }

        long limite = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(30);

        for (Future<Boolean> cierre : cierres) {

            try {
                long restante = Math.max(
                        1L,
                        limite - System.nanoTime()
                );

                cierre.get(restante, TimeUnit.NANOSECONDS);

            } catch (Exception ex) {

                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                log.warn("Cierre KCL incompleto", ex);
            }
        }

        for (Scheduler scheduler : schedulers) {
            try {
                scheduler.shutdown();
            } catch (Exception ex) {
                log.warn("Error cerrando scheduler", ex);
            }
        }

        cerrarCliente(kinesis);
        cerrarCliente(dynamodb);
        cerrarCliente(cloudwatch);

        log.info("KCL_WORKER_DETENIDO worker={}", workerId);
    }

    private void cerrarCliente(AutoCloseable cliente) {

        if (cliente != null) {
            try {
                cliente.close();
            } catch (Exception ex) {
                log.warn("Error cerrando cliente AWS", ex);
            }
        }
    }
}
