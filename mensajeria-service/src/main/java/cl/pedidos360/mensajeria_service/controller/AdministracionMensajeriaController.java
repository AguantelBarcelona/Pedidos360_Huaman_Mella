package cl.pedidos360.mensajeria_service.controller;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mensajeria")
public class AdministracionMensajeriaController {

    private static final Map<String, String> COLAS = Map.of(
            "notificaciones", "pedidos360-notificaciones",
            "documentos", "pedidos360-documentos",
            "auditoria", "pedidos360-auditoria"
    );

    private final SqsClient sqs;

    public AdministracionMensajeriaController(
            @Value("${aws.region:us-east-1}") String region) {

        this.sqs = SqsClient.builder()
                .region(Region.of(region))
                .build();
    }

    // Consultar las seis colas existentes
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

    // Consultar únicamente las tres DLQ
    @GetMapping("/dlq")
    public List<EstadoCola> listarDlq() {
        return List.of(
                consultarCola("pedidos360-notificaciones-dlq"),
                consultarCola("pedidos360-documentos-dlq"),
                consultarCola("pedidos360-auditoria-dlq")
        );
    }

    // Consultar el historial de tareas de reprocesamiento
    @GetMapping("/dlq/{tipo}/tareas")
    public List<EstadoTarea> listarTareas(
            @PathVariable String tipo) {

        String nombreDlq = obtenerNombreDlq(tipo);
        String arnDlq = obtenerArn(nombreDlq);

        return sqs.listMessageMoveTasks(
                ListMessageMoveTasksRequest.builder()
                        .sourceArn(arnDlq)
                        .maxResults(5)
                        .build()
        ).results().stream()
                .map(tarea -> new EstadoTarea(
                        tarea.status(),
                        tarea.approximateNumberOfMessagesMoved(),
                        tarea.approximateNumberOfMessagesToMove(),
                        tarea.failureReason()
                ))
                .toList();
    }

    // Reprocesar mensajes hacia la cola principal
    @PostMapping("/dlq/{tipo}/reprocesar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> reprocesar(
            @PathVariable String tipo,
            @RequestBody Confirmacion solicitud) {

        String origen = obtenerNombreOrigen(tipo);
        String nombreDlq = origen + "-dlq";

        exigirConfirmacion(
                solicitud,
                "REPROCESAR " + nombreDlq
        );

        String tarea = sqs.startMessageMoveTask(
                StartMessageMoveTaskRequest.builder()
                        .sourceArn(obtenerArn(nombreDlq))
                        .destinationArn(obtenerArn(origen))
                        .maxNumberOfMessagesPerSecond(1)
                        .build()
        ).taskHandle();

        return Map.of(
                "estado", "REPROCESAMIENTO_INICIADO",
                "desde", nombreDlq,
                "hacia", origen,
                "tarea", tarea
        );
    }

    // Vaciar definitivamente una DLQ
    @PostMapping("/dlq/{tipo}/vaciar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> vaciar(
            @PathVariable String tipo,
            @RequestBody Confirmacion solicitud) {

        String nombreDlq = obtenerNombreDlq(tipo);

        exigirConfirmacion(
                solicitud,
                "VACIAR " + nombreDlq
        );

        try {
            sqs.purgeQueue(
                    PurgeQueueRequest.builder()
                            .queueUrl(obtenerUrl(nombreDlq))
                            .build()
            );
        } catch (PurgeQueueInProgressException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un vaciado en curso. Espera 60 segundos.",
                    e
            );
        }

        return Map.of(
                "estado", "VACIADO_INICIADO",
                "cola", nombreDlq,
                "aviso", "La eliminacion puede tardar hasta 60 segundos"
        );
    }

    // Obtener el estado de una cola
    private EstadoCola consultarCola(String nombre) {

        Map<QueueAttributeName, String> atributos =
                sqs.getQueueAttributes(
                        GetQueueAttributesRequest.builder()
                                .queueUrl(obtenerUrl(nombre))
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

        return new EstadoCola(nombre, disponibles, enProceso);
    }

    private String obtenerUrl(String nombre) {
        return sqs.getQueueUrl(
                GetQueueUrlRequest.builder()
                        .queueName(nombre)
                        .build()
        ).queueUrl();
    }

    private String obtenerArn(String nombre) {
        return sqs.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                        .queueUrl(obtenerUrl(nombre))
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build()
        ).attributes().get(QueueAttributeName.QUEUE_ARN);
    }

    // Solo se permiten las tres colas de Pedidos360
    private String obtenerNombreOrigen(String tipo) {
        String nombre = COLAS.get(tipo);

        if (nombre == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "DLQ no reconocida: " + tipo
            );
        }

        return nombre;
    }

    private String obtenerNombreDlq(String tipo) {
        return obtenerNombreOrigen(tipo) + "-dlq";
    }

    // Evita operaciones destructivas por accidente
    private void exigirConfirmacion(
            Confirmacion solicitud,
            String textoEsperado) {

        if (solicitud == null ||
                !textoEsperado.equals(solicitud.confirmacion())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Confirmacion requerida: " + textoEsperado
            );
        }
    }

    public record EstadoCola(
            String nombre,
            int mensajesDisponibles,
            int mensajesEnProceso
    ) {}

    public record EstadoTarea(
            String estado,
            Long mensajesMovidos,
            Long mensajesTotales,
            String error
    ) {}

    public record Confirmacion(String confirmacion) {}

    @PreDestroy
    public void cerrar() {
        sqs.close();
    }
}
