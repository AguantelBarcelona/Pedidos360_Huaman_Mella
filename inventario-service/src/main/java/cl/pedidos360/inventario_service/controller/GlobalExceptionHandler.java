package cl.pedidos360.inventario_service.controller;

import cl.pedidos360.inventario_service.service.InventarioNoEncontradoException;
import cl.pedidos360.inventario_service.service.StockInsuficienteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventarioNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarInventarioNoEncontrado(
            InventarioNoEncontradoException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> manejarStockInsuficiente(
            StockInsuficienteException exception) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(
            IllegalArgumentException exception) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("error", exception.getMessage()));
    }
}