package cl.pedidos360.mensajeria_admin_service.kinesis;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesis.model.ResourceInUseException;
import software.amazon.awssdk.services.kinesis.model.InvalidArgumentException;
import software.amazon.awssdk.services.kinesis.model.LimitExceededException;

import java.time.Instant;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
        basePackageClasses = KinesisStreamsController.class
)
public class KinesisAdminExceptionHandler {

    @ExceptionHandler({
            ResourceNotFoundException.class,
            software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException.class
    })
    public ResponseEntity<Map<String, Object>> noExiste(
            AwsServiceException ex) {

        return error(
                HttpStatus.NOT_FOUND,
                "Recurso Kinesis o tabla de checkpoints no encontrada"
        );
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<Map<String, Object>> conflicto(
            ResourceInUseException ex) {

        return error(
                HttpStatus.CONFLICT,
                "El recurso ya existe o tiene una operacion en curso"
        );
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(
            InvalidArgumentException ex) {

        return error(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<Map<String, Object>> limite(
            LimitExceededException ex) {

        return error(
                HttpStatus.TOO_MANY_REQUESTS,
                "Limite de operaciones o capacidad Kinesis excedido"
        );
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus estado,
            String mensaje) {

        return ResponseEntity.status(estado).body(
                Map.of(
                        "status", estado.value(),
                        "error", estado.getReasonPhrase(),
                        "mensaje", mensaje == null ? "" : mensaje,
                        "timestamp", Instant.now().toString()
                )
        );
    }
}
