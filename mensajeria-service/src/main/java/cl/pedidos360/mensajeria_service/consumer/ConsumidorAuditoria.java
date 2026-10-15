
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
public class ConsumidorAuditoria {

    private static final Logger log =
            LoggerFactory.getLogger(ConsumidorAuditoria.class);

    private final SqsClient sqs;
    private final JsonMapper jsonMapper;
    private String queueUrl;

    public ConsumidorAuditoria(
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
                        .queueName("pedidos360-auditoria")
                        .build()
        ).queueUrl();

        log.info("Consumidor SQS iniciado cola=pedidos360-auditoria");
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

            log.error(
                    "Error consultando cola de auditoria: {}",
                    e.getMessage(), e
            );
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
            String eventType = evento.path("eventType").asText();

            if (eventId.isBlank()) {
                throw new IllegalArgumentException(
                        "El evento no tiene eventId"
                );
            }

            if ("PEDIDO_CREADO".equals(eventType)) {

                long pedidoId = evento.path("pedidoId").asLong();

                if (pedidoId <= 0) {
                    throw new IllegalArgumentException(
                            "pedidoId invalido"
                    );
                }

                log.info(
                        "AUDITORIA_PEDIDO eventId={} pedidoId={} intento={} messageId={}",
                        eventId,
                        pedidoId,
                        intento,
                        mensaje.messageId()
                );

            } else if ("PRODUCTO_ACTUALIZADO".equals(eventType)) {

                long productoId = evento.path("productoId").asLong();

                if (productoId <= 0) {
                    throw new IllegalArgumentException(
                            "productoId invalido"
                    );
                }

                log.info(
                        "AUDITORIA_PRODUCTO eventId={} productoId={} intento={} messageId={}",
                        eventId,
                        productoId,
                        intento,
                        mensaje.messageId()
                );

            } else {

                throw new IllegalArgumentException(
                        "Tipo de evento no soportado: " + eventType
                );
            }

            // ACK: eliminar solo despues de registrar el evento.
            sqs.deleteMessage(
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(mensaje.receiptHandle())
                            .build()
            );

            log.info(
                    "SQS_ACK cola=auditoria eventId={} messageId={}",
                    eventId,
                    mensaje.messageId()
            );

        } catch (Exception e) {

            // Sin ACK: SQS reintentara segun la politica de la cola.
            log.error(
                    "SQS_ERROR cola=auditoria messageId={} intento={} razon={}",
                    mensaje.messageId(),
                    intento,
                    e.getMessage()
            );
        }
    }

    @PreDestroy
    public void cerrar() {
        sqs.close();
    }
}
