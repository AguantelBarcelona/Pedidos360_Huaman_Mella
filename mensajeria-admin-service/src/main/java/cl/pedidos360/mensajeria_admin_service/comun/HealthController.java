package cl.pedidos360.mensajeria_admin_service.comun;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "mensajeria-admin-service", "status", "UP");
    }
}
