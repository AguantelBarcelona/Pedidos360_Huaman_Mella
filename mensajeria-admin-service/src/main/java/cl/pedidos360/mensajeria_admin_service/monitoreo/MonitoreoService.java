package cl.pedidos360.mensajeria_admin_service.monitoreo;

import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Metricas SQS (namespace AWS/SQS) y estado de las alarmas del proyecto. */
@Service
public class MonitoreoService {

    /** metrica -> estadistica recomendada por AWS para cada una */
    static final Map<String, String> METRICAS = new LinkedHashMap<>();

    static {
        METRICAS.put("ApproximateNumberOfMessagesVisible", "Maximum");
        METRICAS.put("ApproximateNumberOfMessagesNotVisible", "Maximum");
        METRICAS.put("ApproximateAgeOfOldestMessage", "Maximum");
        METRICAS.put("NumberOfMessagesSent", "Sum");
        METRICAS.put("NumberOfMessagesReceived", "Sum");
        METRICAS.put("NumberOfMessagesDeleted", "Sum");
    }

    private final CloudWatchClient cloudWatch;
    private final NombresRecursos nombres;

    public MonitoreoService(CloudWatchClient cloudWatch, NombresRecursos nombres) {
        this.cloudWatch = cloudWatch;
        this.nombres = nombres;
    }

    public record Serie(String metrica, String estadistica, List<Instant> tiempos, List<Double> valores) {
    }

    public record EstadoAlarma(String nombre, String estado, String motivo, Instant actualizada, String metrica) {
    }

    public List<Serie> metricas(String cola, int minutos) {
        String nombre = nombres.normalizar(cola);
        int rango = Math.max(5, Math.min(minutos, 24 * 60));
        int periodo = rango <= 180 ? 60 : 300;

        List<MetricDataQuery> consultas = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, String> m : METRICAS.entrySet()) {
            consultas.add(MetricDataQuery.builder()
                    .id("m" + i++)
                    .label(m.getKey())
                    .metricStat(MetricStat.builder()
                            .metric(Metric.builder()
                                    .namespace("AWS/SQS")
                                    .metricName(m.getKey())
                                    .dimensions(Dimension.builder().name("QueueName").value(nombre).build())
                                    .build())
                            .period(periodo)
                            .stat(m.getValue())
                            .build())
                    .build());
        }
        Instant fin = Instant.now();
        GetMetricDataResponse respuesta = cloudWatch.getMetricData(GetMetricDataRequest.builder()
                .startTime(fin.minus(Duration.ofMinutes(rango)))
                .endTime(fin)
                .metricDataQueries(consultas)
                .build());

        List<Serie> series = new ArrayList<>();
        for (MetricDataResult r : respuesta.metricDataResults()) {
            series.add(new Serie(r.label(), METRICAS.get(r.label()), r.timestamps(), r.values()));
        }
        return series;
    }

    public List<EstadoAlarma> alarmas() {
        return cloudWatch.describeAlarms(DescribeAlarmsRequest.builder()
                        .alarmNamePrefix(nombres.prefijo() + "-")
                        .build())
                .metricAlarms().stream()
                .map(a -> new EstadoAlarma(a.alarmName(), a.stateValueAsString(), a.stateReason(),
                        a.stateUpdatedTimestamp(), a.metricName()))
                .toList();
    }
}
