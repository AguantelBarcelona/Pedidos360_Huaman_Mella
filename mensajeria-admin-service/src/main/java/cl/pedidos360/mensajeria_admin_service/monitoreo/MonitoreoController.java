package cl.pedidos360.mensajeria_admin_service.monitoreo;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class MonitoreoController {

    private final MonitoreoService service;

    public MonitoreoController(MonitoreoService service) {
        this.service = service;
    }

    @GetMapping("/metricas/{cola}")
    public List<MonitoreoService.Serie> metricas(@PathVariable String cola,
                                                 @RequestParam(defaultValue = "60") int minutos) {
        return service.metricas(cola, minutos);
    }

    @GetMapping("/alarmas")
    public List<MonitoreoService.EstadoAlarma> alarmas() {
        return service.alarmas();
    }
}
