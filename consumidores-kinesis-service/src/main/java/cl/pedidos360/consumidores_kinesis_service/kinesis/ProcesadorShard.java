package cl.pedidos360.consumidores_kinesis_service.kinesis;

import cl.pedidos360.consumidores_kinesis_service.procesamiento.DestinoConsumidor;
import cl.pedidos360.consumidores_kinesis_service.procesamiento.ProcesadorEventos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class ProcesadorShard implements ShardRecordProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(ProcesadorShard.class);

    private final String aplicacion;
    private final String stream;
    private final String workerId;
    private final DestinoConsumidor destino;
    private final ProcesadorEventos procesador;
    private final PublicadorFallidos publicadorFallidos;
    private final int maxIntentos;
    private final long retryDelayMs;
    private final Runnable detenerWorker;

    private final JsonMapper mapper = JsonMapper.builder().build();

    private String shardId;
    private String ultimaSecuencia;
    private long ultimoSubSequence;
    private volatile boolean bloqueado;

    public ProcesadorShard(
            String aplicacion,
            String stream,
            String workerId,
            DestinoConsumidor destino,
            ProcesadorEventos procesador,
            PublicadorFallidos publicadorFallidos,
            int maxIntentos,
            long retryDelayMs,
            Runnable detenerWorker) {

        if (maxIntentos < 1 || maxIntentos > 10
                || retryDelayMs < 0 || retryDelayMs > 30000) {
            throw new IllegalArgumentException(
                    "Configuracion de reintentos invalida");
        }

        this.aplicacion = aplicacion;
        this.stream = stream;
        this.workerId = workerId;
        this.destino = destino;
        this.procesador = procesador;
        this.publicadorFallidos = publicadorFallidos;
        this.maxIntentos = maxIntentos;
        this.retryDelayMs = retryDelayMs;
        this.detenerWorker = detenerWorker;
    }

    @Override
    public void initialize(InitializationInput input) {

        shardId = input.shardId();

        log.info(
                "KCL_SHARD_ASIGNADO app={} worker={} shard={} posicion={}",
                aplicacion,
                workerId,
                shardId,
                input.extendedSequenceNumber()
        );
    }

    @Override
    public void processRecords(ProcessRecordsInput input) {

        if (bloqueado) {
            return;
        }

        try {
            for (KinesisClientRecord registro : input.records()) {

                if (bloqueado) {
                    return;
                }

                String eventId = procesarRegistro(registro);

                checkpointConReintentos(() ->
                        input.checkpointer().checkpoint(
                                registro.sequenceNumber(),
                                registro.subSequenceNumber()
                        )
                );

                ultimaSecuencia = registro.sequenceNumber();
                ultimoSubSequence = registro.subSequenceNumber();

                log.info(
                        "KCL_CHECKPOINT app={} worker={} eventId={} shard={} sequenceNumber={} lagMs={}",
                        aplicacion,
                        workerId,
                        eventId,
                        shardId,
                        ultimaSecuencia,
                        input.millisBehindLatest()
                );
            }

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            bloquear(ex);

        } catch (Exception ex) {

            bloquear(ex);
        }
    }

    private String procesarRegistro(
            KinesisClientRecord registro) throws Exception {

        ByteBuffer buffer = registro.data().asReadOnlyBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        String payload = new String(bytes, StandardCharsets.UTF_8);

        String failureId = UUID.nameUUIDFromBytes(
                (aplicacion + "|" + stream + "|" + shardId + "|"
                        + registro.sequenceNumber() + "|"
                        + registro.subSequenceNumber())
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();

        String eventId = extraerEventId(payload, failureId);

        Exception ultimoError = null;

        for (int intento = 1; intento <= maxIntentos; intento++) {

            try {
                ProcesadorEventos.Resultado resultado =
                        procesador.procesar(
                                aplicacion,
                                destino,
                                registro.partitionKey(),
                                payload
                        );

                log.info(
                        "{} app={} worker={} eventId={} shard={} intento={}",
                        resultado.nuevo()
                                ? "KCL_PROCESADO"
                                : "KCL_DUPLICADO_OMITIDO",
                        aplicacion,
                        workerId,
                        resultado.eventId(),
                        shardId,
                        intento
                );

                return resultado.eventId();

            } catch (Exception ex) {

                ultimoError = ex;

                log.warn(
                        "KCL_ERROR app={} worker={} eventId={} shard={} intento={} causa={}",
                        aplicacion,
                        workerId,
                        eventId,
                        shardId,
                        intento,
                        causa(ex)
                );

                if (intento < maxIntentos) {
                    esperar(intento);
                }
            }
        }

        PublicadorFallidos.EventoFallido fallo =
                new PublicadorFallidos.EventoFallido(
                        failureId,
                        eventId,
                        aplicacion,
                        stream,
                        shardId,
                        registro.sequenceNumber(),
                        registro.subSequenceNumber(),
                        registro.partitionKey(),
                        maxIntentos,
                        causa(ultimoError),
                        Base64.getEncoder().encodeToString(bytes),
                        Instant.now().toString()
                );

        publicarFallidoConReintentos(fallo);

        return eventId;
    }

    private void publicarFallidoConReintentos(
            PublicadorFallidos.EventoFallido fallo) throws Exception {

        for (int intento = 1; intento <= maxIntentos; intento++) {

            try {
                publicadorFallidos.publicar(fallo);
                return;

            } catch (InterruptedException ex) {
                throw ex;

            } catch (Exception ex) {

                log.error(
                        "KCL_ERROR_STREAM_FALLIDOS app={} eventId={} intento={} causa={}",
                        aplicacion,
                        fallo.eventId(),
                        intento,
                        causa(ex)
                );

                if (intento == maxIntentos) {
                    throw ex;
                }

                esperar(intento);
            }
        }
    }

    @FunctionalInterface
    private interface AccionCheckpoint {
        void ejecutar() throws Exception;
    }

    private void checkpointConReintentos(
            AccionCheckpoint accion) throws Exception {

        for (int intento = 1; intento <= maxIntentos; intento++) {

            try {
                accion.ejecutar();
                return;

            } catch (InterruptedException ex) {
                throw ex;

            } catch (Exception ex) {

                log.warn(
                        "KCL_CHECKPOINT_ERROR app={} shard={} intento={} causa={}",
                        aplicacion,
                        shardId,
                        intento,
                        causa(ex)
                );

                if (intento == maxIntentos) {
                    throw ex;
                }

                esperar(intento);
            }
        }
    }

    private String extraerEventId(
            String payload,
            String alternativo) {

        try {
            Map<?, ?> datos = mapper.readValue(payload, Map.class);

            if (datos != null && datos.get("eventId") instanceof String id) {
                return UUID.fromString(id).toString();
            }

        } catch (Exception ignored) {
            // Se usa un identificador determinista si el JSON es invalido.
        }

        return alternativo;
    }

    private void esperar(int intento) throws InterruptedException {

        long demora = Math.min(
                30000L,
                retryDelayMs * (1L << (intento - 1))
        );

        Thread.sleep(demora);
    }

    private String causa(Exception ex) {

        if (ex == null) {
            return "Error desconocido";
        }

        String texto = ex.getClass().getSimpleName()
                + ": " + ex.getMessage();

        texto = texto.replace('\n', ' ').replace('\r', ' ');

        return texto.length() > 300
                ? texto.substring(0, 300)
                : texto;
    }

    private void bloquear(Exception ex) {

        bloqueado = true;

        log.error(
                "KCL_WORKER_BLOQUEADO app={} worker={} shard={} causa={}",
                aplicacion,
                workerId,
                shardId,
                causa(ex)
        );

        detenerWorker.run();
    }

    @Override
    public void leaseLost(LeaseLostInput input) {

        bloqueado = true;

        log.warn(
                "KCL_LEASE_PERDIDO app={} worker={} shard={}",
                aplicacion,
                workerId,
                shardId
        );
    }

    @Override
    public void shardEnded(ShardEndedInput input) {

        if (bloqueado) {
            return;
        }

        try {
            checkpointConReintentos(
                    () -> input.checkpointer().checkpoint()
            );

            log.info(
                    "KCL_SHARD_FINALIZADO app={} worker={} shard={}",
                    aplicacion,
                    workerId,
                    shardId
            );

        } catch (Exception ex) {

            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            bloquear(ex);
        }
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput input) {

        if (bloqueado || ultimaSecuencia == null) {
            return;
        }

        try {
            checkpointConReintentos(() ->
                    input.checkpointer().checkpoint(
                            ultimaSecuencia,
                            ultimoSubSequence
                    )
            );

            log.info(
                    "KCL_CHECKPOINT_CIERRE app={} worker={} shard={} sequenceNumber={}",
                    aplicacion,
                    workerId,
                    shardId,
                    ultimaSecuencia
            );

        } catch (Exception ex) {

            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            bloqueado = true;

            log.warn(
                    "KCL_CIERRE_SIN_CHECKPOINT app={} shard={} causa={}",
                    aplicacion,
                    shardId,
                    causa(ex)
            );
        }
    }
}
