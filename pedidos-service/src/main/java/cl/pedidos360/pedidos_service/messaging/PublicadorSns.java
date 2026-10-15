package cl.pedidos360.pedidos_service.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class PublicadorSns {
    private final SnsClient sns;
    private final String topicArn;

    public PublicadorSns(SnsClient sns,
                        @Value("${app.messaging.sns-topic-arn:}") String topicArn) {
        if (topicArn == null || topicArn.isBlank()) {
            throw new IllegalArgumentException("Configura SNS_TOPIC_ARN para habilitar mensajeria");
        }
        this.sns = sns;
        this.topicArn = topicArn;
    }

    public String publicar(EventoOutbox evento) {
        PublishRequest solicitud = PublishRequest.builder()
                .topicArn(topicArn)
                .message(evento.getPayload())
                .messageAttributes(Map.of(
                        "eventType", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(evento.getEventType())
                                .build()))
                .build();
        return sns.publish(solicitud).messageId();
    }
}
