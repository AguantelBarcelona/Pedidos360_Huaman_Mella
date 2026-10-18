package cl.pedidos360.mensajeria_admin_service.comun;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sqs.model.PurgeQueueInProgressException;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> invalido(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ConfirmacionRequeridaException.class)
    public ResponseEntity<Map<String, Object>> confirmar(ConfirmacionRequeridaException e) {
        return error(HttpStatus.PRECONDITION_REQUIRED, e.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoRecursoException.class)
    public ResponseEntity<Map<String, Object>> fueraDeAlcance(AccesoDenegadoRecursoException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({QueueDoesNotExistException.class, NotFoundException.class})
    public ResponseEntity<Map<String, Object>> noExiste(AwsServiceException e) {
        return error(HttpStatus.NOT_FOUND, mensaje(e));
    }

    @ExceptionHandler(PurgeQueueInProgressException.class)
    public ResponseEntity<Map<String, Object>> purgaEnCurso(PurgeQueueInProgressException e) {
        return error(HttpStatus.CONFLICT, "Ya hay una purga en curso; SQS permite una cada 60 segundos");
    }

    @ExceptionHandler(AwsServiceException.class)
    public ResponseEntity<Map<String, Object>> aws(AwsServiceException e) {
        return error(HttpStatus.BAD_GATEWAY, "AWS: " + mensaje(e));
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<Map<String, Object>> sdk(SdkClientException e) {
        return error(HttpStatus.BAD_GATEWAY, "No se pudo contactar AWS (credenciales/red): " + e.getMessage());
    }

    private static String mensaje(AwsServiceException e) {
        return e.awsErrorDetails() != null && e.awsErrorDetails().errorMessage() != null
                ? e.awsErrorDetails().errorMessage() : e.getMessage();
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "mensaje", mensaje == null ? "" : mensaje,
                "timestamp", Instant.now().toString()));
    }
}
