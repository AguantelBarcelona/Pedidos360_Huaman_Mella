package cl.pedidos360.mensajeria_admin_service.dlq;

import cl.pedidos360.mensajeria_admin_service.colas.ColaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dlq")
public class DlqController {

    private final DlqService service;

    public DlqController(DlqService service) {
        this.service = service;
    }

    @GetMapping
    public List<ColaService.EstadoCola> listar() {
        return service.listar();
    }

    /** GET /api/admin/dlq/notificaciones/mensajes?max=5 */
    @GetMapping("/{tipo}/mensajes")
    public List<DlqService.MensajeDlq> mensajes(@PathVariable String tipo,
                                                @RequestParam(defaultValue = "5") int max) {
        return service.verMensajes(tipo, max);
    }

    /** Body: {"confirmacion":"REPROCESAR pedidos360-notificaciones-dlq"} */
    @PostMapping("/{tipo}/reprocesar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> reprocesar(@PathVariable String tipo,
                                          @RequestBody(required = false) DlqService.Confirmacion solicitud) {
        return service.reprocesar(tipo, solicitud);
    }

    @GetMapping("/{tipo}/tareas")
    public List<DlqService.EstadoTarea> tareas(@PathVariable String tipo) {
        return service.tareas(tipo);
    }

    /** Body: {"confirmacion":"VACIAR pedidos360-notificaciones-dlq"} */
    @PostMapping("/{tipo}/purgar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> purgar(@PathVariable String tipo,
                                      @RequestBody(required = false) DlqService.Confirmacion solicitud) {
        return service.purgar(tipo, solicitud);
    }
}
