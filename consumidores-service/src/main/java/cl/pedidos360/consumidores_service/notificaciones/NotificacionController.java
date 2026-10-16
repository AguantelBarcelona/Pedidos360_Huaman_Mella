package cl.pedidos360.consumidores_service.notificaciones;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionRepository repository;

    public NotificacionController(NotificacionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Notificacion> ultimas() {
        return repository.findTop50ByOrderByEnviadaEnDesc();
    }
}
