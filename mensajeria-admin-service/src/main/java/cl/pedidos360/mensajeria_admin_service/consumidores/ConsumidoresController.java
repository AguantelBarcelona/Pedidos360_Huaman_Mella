package cl.pedidos360.mensajeria_admin_service.consumidores;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/consumidores")
public class ConsumidoresController {

    private final ConsumidoresRemotosService service;

    public ConsumidoresController(ConsumidoresRemotosService service) {
        this.service = service;
    }

    /** Estado de cada instancia de consumidores-service. */
    @GetMapping
    public List<ConsumidoresRemotosService.RespuestaInstancia> estado(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.estado(authorization);
    }

    @PostMapping("/{dominio}/{accion}")
    public List<ConsumidoresRemotosService.RespuestaInstancia> accion(
            @PathVariable String dominio,
            @PathVariable String accion,
            @RequestParam(required = false) String instancia,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.accion(dominio, accion, instancia, authorization);
    }
}
