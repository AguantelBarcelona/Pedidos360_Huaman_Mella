package cl.pedidos360.consumidores_service.controller;

import cl.pedidos360.consumidores_service.comun.sqs.ConsumidoresProperties;
import cl.pedidos360.consumidores_service.comun.sqs.RegistroConsumidores;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final ConsumidoresProperties props;
    private final RegistroConsumidores registro;

    public HealthController(ConsumidoresProperties props, RegistroConsumidores registro) {
        this.props = props;
        this.registro = registro;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "consumidores-service",
                "status", "UP",
                "instancia", props.instancia(),
                "consumidoresActivos", registro.todos().size());
    }
}
