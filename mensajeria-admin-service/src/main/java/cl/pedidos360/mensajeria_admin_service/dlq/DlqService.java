package cl.pedidos360.mensajeria_admin_service.dlq;

import cl.pedidos360.mensajeria_admin_service.colas.ColaService;
import cl.pedidos360.mensajeria_admin_service.comun.AdminProperties;
import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.ConfirmacionRequeridaException;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ListMessageMoveTasksRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.StartMessageMoveTaskRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class DlqService {

    private final SqsClient sqs;
    private final ColaService colas;
    private final NombresRecursos nombres;
    private final AuditoriaAdmin auditoria;
    private final AdminProperties props;

    public DlqService(SqsClient sqs, ColaService colas, NombresRecursos nombres,
                      AuditoriaAdmin auditoria, AdminProperties props) {
        this.sqs = sqs;
        this.colas = colas;
        this.nombres = nombres;
        this.auditoria = auditoria;
        this.props = props;
    }

    public record Confirmacion(String confirmacion) {
    }

    public record EstadoTarea(String estado, Long mensajesMovidos, Long mensajesTotales, String error) {
    }

    public record MensajeDlq(String messageId, int vecesRecibido, String cuerpo, Map<String, String> atributos) {
    }

    public List<ColaService.EstadoCola> listar() {
        return colas.listar().stream().filter(ColaService.EstadoCola::esDlq).toList();
    }

    public List<MensajeDlq> verMensajes(String tipo, int max) {
        String dlqUrl = colas.url(nombreDlq(tipo));
        List<Message> mensajes = sqs.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(dlqUrl)
                .maxNumberOfMessages(Math.max(1, Math.min(max, 10)))
                .waitTimeSeconds(1)
                .visibilityTimeout(0)
                .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                .messageAttributeNames("All")
                .build()).messages();
        Map<String, MensajeDlq> unicos = new LinkedHashMap<>();
        for (Message m : mensajes) {
            Map<String, String> atributos = new LinkedHashMap<>();
            m.messageAttributes().forEach((k, v) -> atributos.put(k, v.stringValue()));
            String veces = m.attributes().getOrDefault(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "0");
            unicos.put(m.messageId(), new MensajeDlq(m.messageId(), Integer.parseInt(veces), m.body(), atributos));
        }
        return List.copyOf(unicos.values());
    }

    public Map<String, String> reprocesar(String tipo, Confirmacion solicitud) {
        String dlq = nombreDlq(tipo);
        String origen = nombres.colaOrigenDeDlq(dlq);
        exigir(solicitud, "REPROCESAR " + dlq);

        String tarea = sqs.startMessageMoveTask(StartMessageMoveTaskRequest.builder()
                .sourceArn(colas.arn(colas.url(dlq)))
                .destinationArn(colas.arn(colas.url(origen)))
                .maxNumberOfMessagesPerSecond(1)
                .build()).taskHandle();

        auditoria.registrar("REPROCESAR_DLQ", dlq, "hacia=" + origen + " tarea=" + tarea);
        return Map.of("estado", "REPROCESAMIENTO_INICIADO", "desde", dlq, "hacia", origen, "tarea", tarea);
    }

    public List<EstadoTarea> tareas(String tipo) {
        String dlq = nombreDlq(tipo);
        return sqs.listMessageMoveTasks(ListMessageMoveTasksRequest.builder()
                        .sourceArn(colas.arn(colas.url(dlq)))
                        .maxResults(5)
                        .build())
                .results().stream()
                .map(t -> new EstadoTarea(t.status(), t.approximateNumberOfMessagesMoved(),
                        t.approximateNumberOfMessagesToMove(), t.failureReason()))
                .toList();
    }

    public Map<String, String> purgar(String tipo, Confirmacion solicitud) {
        if (!props.permitirPurga()) {
            throw new IllegalArgumentException("La purga esta deshabilitada (PERMITIR_PURGA_DLQ=false)");
        }
        String dlq = nombreDlq(tipo);
        exigir(solicitud, "VACIAR " + dlq);

        sqs.purgeQueue(PurgeQueueRequest.builder().queueUrl(colas.url(dlq)).build());
        auditoria.registrar("PURGAR_DLQ", dlq, "");
        return Map.of("estado", "VACIADO_INICIADO", "cola", dlq,
                "aviso", "SQS puede tardar hasta 60 segundos en eliminar los mensajes");
    }

    String nombreDlq(String tipo) {
        String base = nombres.normalizar(tipo);
        return nombres.esDlq(base) ? base : base + NombresRecursos.SUFIJO_DLQ;
    }

    private static void exigir(Confirmacion solicitud, String esperado) {
        if (solicitud == null || !esperado.equals(solicitud.confirmacion())) {
            throw new ConfirmacionRequeridaException(
                    "Operacion destructiva: envie {\"confirmacion\":\"" + esperado + "\"}");
        }
    }
}
