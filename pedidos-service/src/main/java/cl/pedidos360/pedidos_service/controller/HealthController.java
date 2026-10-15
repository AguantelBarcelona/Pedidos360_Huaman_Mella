package cl.pedidos360.pedidos_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {

        return Map.of(
                "service", "pedidos-service",
                "status", "UP"
        );
    }
}