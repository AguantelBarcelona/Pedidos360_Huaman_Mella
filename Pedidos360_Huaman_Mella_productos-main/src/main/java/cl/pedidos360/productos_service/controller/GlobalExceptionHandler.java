package cl.pedidos360.productos_service.controller;

import cl.pedidos360.productos_service.service.ProductoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(
            ProductoNoEncontradoException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(cuerpoError(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
            MethodArgumentNotValidException ex) {

        String detalle = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField()
                        + ": "
                        + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(cuerpoError(detalle));
    }

    private Map<String, Object> cuerpoError(String mensaje) {

        Map<String, Object> cuerpo = new LinkedHashMap<>();

        cuerpo.put("timestamp", Instant.now().toString());
        cuerpo.put("mensaje", mensaje);

        return cuerpo;
    }
}