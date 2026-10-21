package cl.pedidos360.mensajeria_admin_service.topicos;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/topicos")
public class TopicoController {

    private final TopicoService service;

    public TopicoController(TopicoService service) {
        this.service = service;
    }

    public record CrearTopicoRequest(String nombre) {
    }

    @GetMapping
    public List<TopicoService.TopicoInfo> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicoService.TopicoInfo crear(@RequestBody CrearTopicoRequest request) {
        return service.crear(request.nombre());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestParam String arn, @RequestParam(required = false) String confirmar) {
        service.eliminar(arn, confirmar);
    }
}
