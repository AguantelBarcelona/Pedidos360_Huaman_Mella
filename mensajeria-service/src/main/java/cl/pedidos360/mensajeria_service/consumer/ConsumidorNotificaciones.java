
package cl.pedidos360.mensajeria_service.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(
        name = "mensajeria.enabled",
        havingValue = "true"
)
public class ConsumidorNotificaciones {

    private static final Logger log =
            LoggerFactory.getLogger(ConsumidorNotificaciones.class);

    private final SqsClient sqs;
    private final JsonMapper jsonMapper;
    private String queueUrl;

    public ConsumidorNotificaciones(
            JsonMapper jsonMapper,
            @Value("${aws.region:us-east-1}") String region) {

        this.jsonMapper = jsonMapper;
        this.sqs = SqsClient.builder()
                .region(Region.of(region))
                .build();
    }

    @PostConstruct
    public void iniciar() {
        queueUrl = sqs.getQueueUrl(
                GetQueueUrlRequest.builder()
                        .queueName("pedidos360-notificaciones")
                        .build()
        ).queueUrl();

        log.info("Consumidor SQS iniciado cola=pedidos360-notificaciones");
    }

    @Scheduled(fixedDelayString = "${mensajeria.poll-delay-ms:5000}")
    public void consultarMensajes() {

        try {
            var respuesta = sqs.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(5)
                            .waitTimeSeconds(10)
                            .visibilityTimeout(60)
                            .messageSystemAttributeNames(
                                    MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT
                            )
                            .build()
            );

            for (Message mensaje : respuesta.messages()) {
                procesar(mensaje);
            }

        } catch (Exception e) {
            log.error("Error consultando cola de notificaciones: {}",
                    e.getMessage(), e);
        }
    }

    private void procesar(Message mensaje) {

        String intento = mensaje.attributes().getOrDefault(
                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                "1"
        );

        try {
            JsonNode evento = jsonMapper.readTree(mensaje.body());

            String eventId = evento.path("eventId").asText();
            String tipo = evento.path("eventType").asText();
            long pedidoId = evento.path("pedidoId").asLong();

            if (!"PEDIDO_CREADO".equals(tipo)
                    || eventId.isBlank()
                    || pedidoId <= 0) {

                throw new IllegalArgumentException(
                        "Evento invalido para notificaciones"
                );
            }

            // Primera etapa: validar y registrar la notificacion.
            log.info(
                    "NOTIFICACION_PROCESADA eventId={} pedidoId={} intento={} messageId={}",
                    eventId, pedidoId, intento, mensaje.messageId()
            );

            // ACK: eliminar de SQS solo despues del procesamiento exitoso.
            sqs.deleteMessage(
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(mensaje.receiptHandle())
                            .build()
            );

            log.info(
                    "SQS_ACK eventId={} messageId={}",
                    eventId, mensaje.messageId()
            );

        } catch (Exception e) {

            // Sin ACK: SQS podra reintentar y luego aplicar la DLQ.
            log.error(
                    "SQS_ERROR messageId={} intento={} razon={}",
                    mensaje.messageId(), intento, e.getMessage()
            );
        }
    }

    @PreDestroy
    public void cerrar() {
        sqs.close();
    }
}
