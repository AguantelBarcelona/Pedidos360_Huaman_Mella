package cl.pedidos360.mensajeria_service.controller;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mensajeria")
public class AdministracionMensajeriaController {

    private final SqsClient sqs;

    public AdministracionMensajeriaController(
            @Value("${aws.region:us-east-1}") String region) {

        this.sqs = SqsClient.builder()
                .region(Region.of(region))
                .build();
    }

    @GetMapping("/colas")
    public List<EstadoCola> listarColas() {

        return List.of(
                consultarCola("pedidos360-notificaciones"),
                consultarCola("pedidos360-documentos"),
                consultarCola("pedidos360-auditoria"),
                consultarCola("pedidos360-notificaciones-dlq"),
                consultarCola("pedidos360-documentos-dlq"),
                consultarCola("pedidos360-auditoria-dlq")
        );
    }

    private EstadoCola consultarCola(String nombre) {

        String url = sqs.getQueueUrl(
                GetQueueUrlRequest.builder()
                        .queueName(nombre)
                        .build()
        ).queueUrl();

        Map<QueueAttributeName, String> atributos =
                sqs.getQueueAttributes(
                        GetQueueAttributesRequest.builder()
                                .queueUrl(url)
                                .attributeNames(
                                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE
                                )
                                .build()
                ).attributes();

        int disponibles = Integer.parseInt(
                atributos.getOrDefault(
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                        "0"
                )
        );

        int enProceso = Integer.parseInt(
                atributos.getOrDefault(
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                        "0"
                )
        );

        return new EstadoCola(
                nombre,
                disponibles,
                enProceso
        );
    }

    public record EstadoCola(
            String nombre,
            int mensajesDisponibles,
            int mensajesEnProceso
    ) {}

    @PreDestroy
    public void cerrar() {
        sqs.close();
    }
}
