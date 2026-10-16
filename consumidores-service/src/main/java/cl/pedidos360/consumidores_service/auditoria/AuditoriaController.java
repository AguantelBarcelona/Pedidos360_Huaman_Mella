package cl.pedidos360.consumidores_service.auditoria;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final RegistroAuditoriaRepository repository;

    public AuditoriaController(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<RegistroAuditoria> ultimos(@RequestParam(required = false) String eventType) {
        return eventType == null || eventType.isBlank()
                ? repository.findTop100ByOrderByRegistradoEnDesc()
                : repository.findTop100ByEventTypeOrderByRegistradoEnDesc(eventType);
    }
}
