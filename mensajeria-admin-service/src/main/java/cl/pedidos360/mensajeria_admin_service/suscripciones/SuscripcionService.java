package cl.pedidos360.mensajeria_admin_service.suscripciones;

import cl.pedidos360.mensajeria_admin_service.colas.ColaService;
import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

@Service
public class SuscripcionService {

    private final SnsClient sns;
    private final SqsClient sqs;
    private final ColaService colas;
    private final NombresRecursos nombres;
    private final AuditoriaAdmin auditoria;
    private final JsonMapper json;

    public SuscripcionService(SnsClient sns, SqsClient sqs, ColaService colas, NombresRecursos nombres,
                              AuditoriaAdmin auditoria, JsonMapper json) {
        this.sns = sns;
        this.sqs = sqs;
        this.colas = colas;
        this.nombres = nombres;
        this.auditoria = auditoria;
        this.json = json;
    }

    /** eventTypes vacio o null = la cola recibe todos los eventos del topico (sin filtro). */
    public record CrearSuscripcionRequest(String topicArn, String cola, List<String> eventTypes) {
    }

    public record SuscripcionInfo(String arn, String topicArn, String protocolo, String endpoint,
                                  String filtro, String rawMessageDelivery) {
    }

    public List<SuscripcionInfo> listar(String topicArn) {
        nombres.validarArn(topicArn);
        return sns.listSubscriptionsByTopicPaginator(ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build())
                .subscriptions().stream()
                .map(s -> {
                    Map<String, String> a = s.subscriptionArn().startsWith("arn:")
                            ? sns.getSubscriptionAttributes(GetSubscriptionAttributesRequest.builder()
                                    .subscriptionArn(s.subscriptionArn()).build()).attributes()
                            : Map.of();
                    return new SuscripcionInfo(s.subscriptionArn(), s.topicArn(), s.protocol(), s.endpoint(),
                            a.get("FilterPolicy"), a.get("RawMessageDelivery"));
                })
                .toList();
    }

    public SuscripcionInfo crear(CrearSuscripcionRequest req) {
        nombres.validarArn(req.topicArn());
        String cola = nombres.normalizar(req.cola());
        String colaUrl = colas.url(cola);
        String colaArn = colas.arn(colaUrl);

        // 1) La cola debe permitir que ESTE topico le envie mensajes
        permitirTopico(colaUrl, colaArn, req.topicArn());

        // 2) Suscripcion con entrega raw y filtro por atributo eventType
        SubscribeRequest.Builder sub = SubscribeRequest.builder()
                .topicArn(req.topicArn())
                .protocol("sqs")
                .endpoint(colaArn)
                .returnSubscriptionArn(true);
        String filtro = filtro(req.eventTypes());
        if (filtro != null) {
            sub.attributes(Map.of(
                    "RawMessageDelivery", "true",
                    "FilterPolicyScope", "MessageAttributes",
                    "FilterPolicy", filtro));
        } else {
            sub.attributes(Map.of("RawMessageDelivery", "true"));
        }
        String arn = sns.subscribe(sub.build()).subscriptionArn();
        auditoria.registrar("CREAR_SUSCRIPCION", arn, "cola=" + cola + " filtro=" + filtro);
        return new SuscripcionInfo(arn, req.topicArn(), "sqs", colaArn, filtro, "true");
    }

    public void eliminar(String suscripcionArn) {
        nombres.validarArn(suscripcionArn);
        sns.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(suscripcionArn).build());
        auditoria.registrar("ELIMINAR_SUSCRIPCION", suscripcionArn, "");
    }

    String filtro(List<String> eventTypes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return null;
        }
        ObjectNode raiz = json.createObjectNode();
        ArrayNode valores = raiz.putArray("eventType");
        eventTypes.forEach(t -> {
            if (t == null || !t.matches("^[A-Z0-9_]{1,80}$")) {
                throw new IllegalArgumentException("eventType invalido: " + t);
            }
            valores.add(t);
        });
        return json.writeValueAsString(raiz);
    }

    /** Agrega (sin borrar las existentes) una declaracion a la politica de la cola. */
    void permitirTopico(String colaUrl, String colaArn, String topicArn) {
        String actual = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(colaUrl).attributeNames(QueueAttributeName.POLICY).build())
                .attributesAsStrings().get("Policy");
        ObjectNode politica;
        if (actual == null || actual.isBlank()) {
            politica = json.createObjectNode();
            politica.put("Version", "2012-10-17");
            politica.putArray("Statement");
        } else {
            politica = (ObjectNode) json.readTree(actual);
        }
        JsonNode existentes = politica.get("Statement");
        ArrayNode statements;
        if (existentes instanceof ArrayNode arr) {
            statements = arr;
        } else {
            statements = json.createArrayNode();
            if (existentes != null) statements.add(existentes);
            politica.set("Statement", statements);
        }
        for (JsonNode s : statements) {
            if (s.toString().contains(topicArn) && s.toString().contains(colaArn)) {
                return; // ya permitido
            }
        }
        ObjectNode st = statements.addObject();
        st.put("Sid", "Sns" + Math.abs(topicArn.hashCode()));
        st.put("Effect", "Allow");
        st.putObject("Principal").put("Service", "sns.amazonaws.com");
        st.put("Action", "sqs:SendMessage");
        st.put("Resource", colaArn);
        st.putObject("Condition").putObject("ArnEquals").put("aws:SourceArn", topicArn);

        sqs.setQueueAttributes(SetQueueAttributesRequest.builder()
                .queueUrl(colaUrl)
                .attributesWithStrings(Map.of("Policy", json.writeValueAsString(politica)))
                .build());
    }
}
