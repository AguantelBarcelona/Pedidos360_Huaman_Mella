package cl.pedidos360.mensajeria_service.consumer;

import cl.pedidos360.mensajeria_service.service.RegistroMensajesProcesados;
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
public class ConsumidorDocumentos {

    private static final Logger log =
            LoggerFactory.getLogger(ConsumidorDocumentos.class);

    private final SqsClient sqs;
    private final JsonMapper jsonMapper;
    private final RegistroMensajesProcesados registro;

    private String queueUrl;

    public ConsumidorDocumentos(
            JsonMapper jsonMapper,
            RegistroMensajesProcesados registro,
            @Value("${aws.region:us-east-1}") String region) {

        this.jsonMapper = jsonMapper;
        this.registro = registro;

        this.sqs = SqsClient.builder()
                .region(Region.of(region))
                .build();
    }

    @PostConstruct
    public void iniciar() {

        queueUrl = sqs.getQueueUrl(
                GetQueueUrlRequest.builder()
                        .queueName("pedidos360-documentos")
                        .build()
        ).queueUrl();

        log.info("Consumidor iniciado cola=pedidos360-documentos");
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
                    "SQS_CONSULTA_ERROR cola=documentos razon={}",
                    e.getMessage(),
                    e
            );
        }
    }

    private void procesar(Message mensaje) {

        String intento = mensaje.attributes().getOrDefault(
                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                "1"
        );

        String eventId = "desconocido";

        try {
            JsonNode evento = extraerEvento(mensaje.body());

            eventId = evento.path("eventId").asText();
            String eventType = evento.path("eventType").asText();
            long pedidoId = evento.path("pedidoId").asLong();

            if (eventId.isBlank()
                    || !"PEDIDO_CREADO".equals(eventType)
                    || pedidoId <= 0) {

                throw new IllegalArgumentException(
                        "Evento PEDIDO_CREADO invalido"
                );
            }

            boolean esNuevo = registro.registrarSiNuevo(
                    eventId,
                    "documentos",
                    eventType,
                    mensaje.messageId()
            );

            if (esNuevo) {

                // Etapa actual: registrar la solicitud documental.
                log.info(
                        "DOCUMENTO_PROCESADO eventId={} pedidoId={} intento={} messageId={}",
                        eventId,
                        pedidoId,
                        intento,
                        mensaje.messageId()
                );

            } else {

                log.info(
                        "SQS_DUPLICADO cola=documentos eventId={} intento={} messageId={}",
                        eventId,
                        intento,
                        mensaje.messageId()
                );
            }

            // ACK solo despues del registro correcto en PostgreSQL.
            sqs.deleteMessage(
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(mensaje.receiptHandle())
                            .build()
            );

            log.info(
                    "SQS_ACK cola=documentos eventId={} messageId={}",
                    eventId,
                    mensaje.messageId()
            );

        } catch (Exception e) {

            // Sin ACK: SQS reintentara y podra enviar a DLQ.
            log.error(
                    "SQS_ERROR cola=documentos eventId={} messageId={} intento={} razon={}",
                    eventId,
                    mensaje.messageId(),
                    intento,
                    e.getMessage()
            );
        }
    }

    private JsonNode extraerEvento(String cuerpo) throws Exception {

        JsonNode raiz = jsonMapper.readTree(cuerpo);

        // Mensaje recibido con envoltorio SNS.
        if ("Notification".equals(raiz.path("Type").asText())
                && !raiz.path("Message").isMissingNode()) {

            return jsonMapper.readTree(
                    raiz.path("Message").asText()
            );
        }

        // Mensaje directo o Raw Message Delivery.
        return raiz;
    }

    @PreDestroy
    public void cerrar() {
        sqs.close();
    }
}
