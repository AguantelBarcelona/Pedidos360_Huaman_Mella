package cl.pedidos360.mensajeria_admin_service.suscripciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suscripciones")
public class SuscripcionController {

    private final SuscripcionService service;

    public SuscripcionController(SuscripcionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SuscripcionService.SuscripcionInfo> listar(@RequestParam String topicArn) {
        return service.listar(topicArn);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuscripcionService.SuscripcionInfo crear(@RequestBody SuscripcionService.CrearSuscripcionRequest request) {
        return service.crear(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestParam String arn) {
        service.eliminar(arn);
    }
}
