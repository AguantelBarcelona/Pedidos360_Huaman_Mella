package cl.pedidos360.consumidores_service.comun.sqs;

import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;
import cl.pedidos360.consumidores_service.comun.fallos.RegistroFalloRepository;
import cl.pedidos360.consumidores_service.comun.fallos.SimuladorFallos;
import cl.pedidos360.consumidores_service.comun.idempotencia.TransaccionConsumo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Busca los beans anotados con {@link EscuchaSqs}, crea un {@link ConsumidorSqs}
 * por cada dominio activo y lo arranca/detiene junto con la aplicacion
 * (apagado ordenado incluido).
 */
@Component
public class RegistroConsumidores implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RegistroConsumidores.class);

    private final ApplicationContext contexto;
    private final ConsumidoresProperties props;
    private final ObjectProvider<SqsClient> sqs;
    private final JsonMapper json;
    private final TransaccionConsumo transaccion;
    private final RegistroFalloRepository fallos;
    private final SimuladorFallos simulador;

    private final Map<String, ConsumidorSqs> consumidores = new LinkedHashMap<>();
    private volatile boolean corriendo;

    public RegistroConsumidores(ApplicationContext contexto, ConsumidoresProperties props,
                                ObjectProvider<SqsClient> sqs, JsonMapper json,
                                TransaccionConsumo transaccion, RegistroFalloRepository fallos,
                                SimuladorFallos simulador) {
        this.contexto = contexto;
        this.props = props;
        this.sqs = sqs;
        this.json = json;
        this.transaccion = transaccion;
        this.fallos = fallos;
        this.simulador = simulador;
    }

    @Override
    public synchronized void start() {
        corriendo = true;
        if (!props.enabled()) {
            log.warn("CONSUMIDORES_ENABLED=false: el servicio no leera colas SQS");
            return;
        }
        SqsClient cliente = sqs.getObject();
        for (Map.Entry<String, Object> bean : contexto.getBeansWithAnnotation(EscuchaSqs.class).entrySet()) {
            if (!(bean.getValue() instanceof ProcesadorEvento procesador)) {
                throw new IllegalStateException("El bean '" + bean.getKey()
                        + "' tiene @EscuchaSqs pero no implementa ProcesadorEvento");
            }
            String dominio = procesador.dominio();
            if (!props.activos().contains(dominio)) {
                log.info("consumidor={} no activo en esta instancia (CONSUMIDORES_ACTIVOS)", dominio);
                continue;
            }
            ConsumidoresProperties.Cola cola = props.colas().get(dominio);
            if (cola == null || cola.url() == null || cola.url().isBlank()) {
                log.warn("consumidor={} sin URL de cola configurada; se omite", dominio);
                continue;
            }
            ConsumidorSqs consumidor = new ConsumidorSqs(dominio, cola.url(), cola.dlqUrl(), procesador,
                    cliente, json, transaccion, fallos, simulador, props);
            consumidores.put(dominio, consumidor);
            consumidor.iniciar();
        }
    }

    @Override
    public synchronized void stop() {
        consumidores.values().forEach(ConsumidorSqs::detener);
        corriendo = false;
    }

    @Override
    public boolean isRunning() {
        return corriendo;
    }

    public Optional<ConsumidorSqs> obtener(String dominio) {
        return Optional.ofNullable(consumidores.get(dominio));
    }

    public Collection<ConsumidorSqs> todos() {
        return consumidores.values();
    }
}
