package cl.pedidos360.mensajeria_admin_service.topicos;

import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.Topic;

import java.util.List;
import java.util.Map;

@Service
public class TopicoService {

    private final SnsClient sns;
    private final NombresRecursos nombres;
    private final AuditoriaAdmin auditoria;

    public TopicoService(SnsClient sns, NombresRecursos nombres, AuditoriaAdmin auditoria) {
        this.sns = sns;
        this.nombres = nombres;
        this.auditoria = auditoria;
    }

    public record TopicoInfo(String nombre, String arn, String suscripcionesConfirmadas, String suscripcionesPendientes) {
    }

    public List<TopicoInfo> listar() {
        return sns.listTopicsPaginator(ListTopicsRequest.builder().build()).topics().stream()
                .map(Topic::topicArn)
                .filter(arn -> nombreDe(arn).startsWith(nombres.prefijo() + "-"))
                .map(this::info)
                .toList();
    }

    public TopicoInfo crear(String nombre) {
        String completo = nombres.normalizar(nombre);
        String arn = sns.createTopic(CreateTopicRequest.builder()
                .name(completo)
                .tags(software.amazon.awssdk.services.sns.model.Tag.builder().key("Proyecto").value("Pedidos360").build())
                .build()).topicArn();
        auditoria.registrar("CREAR_TOPICO", arn, "");
        return info(arn);
    }

    public void eliminar(String arn, String confirmar) {
        nombres.validarArn(arn);
        nombres.exigirConfirmacion(nombreDe(arn), confirmar);
        sns.deleteTopic(DeleteTopicRequest.builder().topicArn(arn).build());
        auditoria.registrar("ELIMINAR_TOPICO", arn, "");
    }

    private TopicoInfo info(String arn) {
        Map<String, String> a = sns.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(arn).build()).attributes();
        return new TopicoInfo(nombreDe(arn), arn, a.get("SubscriptionsConfirmed"), a.get("SubscriptionsPending"));
    }

    static String nombreDe(String arn) {
        return arn.substring(arn.lastIndexOf(':') + 1);
    }
}
