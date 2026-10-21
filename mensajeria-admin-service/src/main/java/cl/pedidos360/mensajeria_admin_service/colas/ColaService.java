package cl.pedidos360.mensajeria_admin_service.colas;

import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ColaService {

    private final SqsClient sqs;
    private final NombresRecursos nombres;
    private final AuditoriaAdmin auditoria;

    public ColaService(SqsClient sqs, NombresRecursos nombres, AuditoriaAdmin auditoria) {
        this.sqs = sqs;
        this.nombres = nombres;
        this.auditoria = auditoria;
    }

    /** origen 4 dias / visibilidad 120 s / long polling 20 s; DLQ 14 dias; maxReceiveCount 3.*/
    public record CrearColaRequest(String nombre, Boolean conDlq, Integer maxReceiveCount,
                                   Integer visibilidadSegundos, Integer retencionSegundos) {
    }

    public record EstadoCola(String nombre, String url, String arn, boolean esDlq,
                             long disponibles, long enProceso, long retrasados,
                             int visibilidadSegundos, int retencionSegundos, int esperaRecepcionSegundos,
                             String dlqArn, Integer maxReceiveCount, Map<String, String> atributos) {
    }

    public List<EstadoCola> listar() {
        return sqs.listQueues(ListQueuesRequest.builder().queueNamePrefix(nombres.prefijo() + "-").build())
                .queueUrls().stream().map(this::estadoPorUrl).toList();
    }

    public EstadoCola estado(String nombre) {
        return estadoPorUrl(url(nombres.normalizar(nombre)));
    }

    public String url(String nombreCompleto) {
        return sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(nombreCompleto).build()).queueUrl();
    }

    public String arn(String url) {
        return sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(url).attributeNames(QueueAttributeName.QUEUE_ARN).build())
                .attributesAsStrings().get("QueueArn");
    }

    public EstadoCola crear(CrearColaRequest req) {
        String nombre = nombres.normalizar(req.nombre());
        if (nombres.esDlq(nombre)) {
            throw new IllegalArgumentException("Cree la cola origen; la DLQ '-dlq' se crea automaticamente");
        }
        int maxReceive = req.maxReceiveCount() == null ? 3 : req.maxReceiveCount();
        int visibilidad = req.visibilidadSegundos() == null ? 120 : req.visibilidadSegundos();
        int retencion = req.retencionSegundos() == null ? 345_600 : req.retencionSegundos();
        if (maxReceive < 1 || maxReceive > 1000) throw new IllegalArgumentException("maxReceiveCount entre 1 y 1000");
        if (visibilidad < 0 || visibilidad > 43_200) throw new IllegalArgumentException("visibilidad entre 0 y 43200 s");
        if (retencion < 60 || retencion > 1_209_600) throw new IllegalArgumentException("retencion entre 60 y 1209600 s");

        Map<String, String> atributos = new LinkedHashMap<>();
        atributos.put("MessageRetentionPeriod", String.valueOf(retencion));
        atributos.put("VisibilityTimeout", String.valueOf(visibilidad));
        atributos.put("ReceiveMessageWaitTimeSeconds", "20");
        atributos.put("SqsManagedSseEnabled", "true");

        if (req.conDlq() == null || req.conDlq()) {
            String dlqUrl = sqs.createQueue(CreateQueueRequest.builder()
                    .queueName(nombre + NombresRecursos.SUFIJO_DLQ)
                    .attributesWithStrings(Map.of(
                            // La DLQ retiene 14 dias
                            "MessageRetentionPeriod", "1209600",
                            "SqsManagedSseEnabled", "true"))
                    .build()).queueUrl();
            atributos.put("RedrivePolicy",
                    "{\"deadLetterTargetArn\":\"" + arn(dlqUrl) + "\",\"maxReceiveCount\":\"" + maxReceive + "\"}");
            auditoria.registrar("CREAR_DLQ", nombre + NombresRecursos.SUFIJO_DLQ, "retencion=1209600");
        }

        String url = sqs.createQueue(CreateQueueRequest.builder()
                .queueName(nombre)
                .attributesWithStrings(atributos)
                .build()).queueUrl();
        auditoria.registrar("CREAR_COLA", nombre, atributos.toString());
        return estadoPorUrl(url);
    }

    /** Elimina la cola y tambien su DLQ. */
    public void eliminar(String nombre, String confirmar, boolean incluirDlq) {
        String completo = nombres.normalizar(nombre);
        nombres.exigirConfirmacion(completo, confirmar);
        sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(url(completo)).build());
        auditoria.registrar("ELIMINAR_COLA", completo, "");
        if (incluirDlq && !nombres.esDlq(completo)) {
            String dlq = completo + NombresRecursos.SUFIJO_DLQ;
            try {
                sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(url(dlq)).build());
                auditoria.registrar("ELIMINAR_COLA", dlq, "dlq asociada");
            } catch (QueueDoesNotExistException e) {
            }
        }
    }

    EstadoCola estadoPorUrl(String url) {
        Map<String, String> a = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(url).attributeNames(QueueAttributeName.ALL).build()).attributesAsStrings();
        String nombre = url.substring(url.lastIndexOf('/') + 1);
        String dlqArn = null;
        Integer maxReceive = null;
        String redrive = a.get("RedrivePolicy");
        if (redrive != null) {
            dlqArn = extraer(redrive, "deadLetterTargetArn");
            String m = extraer(redrive, "maxReceiveCount");
            maxReceive = m == null ? null : Integer.valueOf(m);
        }
        return new EstadoCola(nombre, url, a.get("QueueArn"), nombres.esDlq(nombre),
                largo(a, "ApproximateNumberOfMessages"),
                largo(a, "ApproximateNumberOfMessagesNotVisible"),
                largo(a, "ApproximateNumberOfMessagesDelayed"),
                (int) largo(a, "VisibilityTimeout"),
                (int) largo(a, "MessageRetentionPeriod"),
                (int) largo(a, "ReceiveMessageWaitTimeSeconds"),
                dlqArn, maxReceive, a);
    }

    private static long largo(Map<String, String> a, String clave) {
        String v = a.get(clave);
        return v == null ? 0 : Long.parseLong(v);
    }

    static String extraer(String json, String campo) {
        int i = json.indexOf("\"" + campo + "\"");
        if (i < 0) return null;
        int dosPuntos = json.indexOf(':', i);
        int fin = dosPuntos + 1;
        while (fin < json.length() && Character.isWhitespace(json.charAt(fin))) fin++;
        if (json.charAt(fin) == '"') {
            return json.substring(fin + 1, json.indexOf('"', fin + 1));
        }
        int hasta = fin;
        while (hasta < json.length() && (Character.isDigit(json.charAt(hasta)))) hasta++;
        return json.substring(fin, hasta);
    }
}
