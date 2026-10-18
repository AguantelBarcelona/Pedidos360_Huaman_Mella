package cl.pedidos360.mensajeria_admin_service.colas;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/colas")
public class ColaController {

    private final ColaService service;

    public ColaController(ColaService service) {
        this.service = service;
    }

    /** Estado de todas las colas del proyecto (incluye DLQ). */
    @GetMapping
    public List<ColaService.EstadoCola> listar() {
        return service.listar();
    }

    @GetMapping("/{nombre}")
    public ColaService.EstadoCola estado(@PathVariable String nombre) {
        return service.estado(nombre);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColaService.EstadoCola crear(@RequestBody ColaService.CrearColaRequest request) {
        return service.crear(request);
    }

    /** DELETE /api/admin/colas/pedidos360-x?confirmar=pedidos360-x&incluirDlq=true */
    @DeleteMapping("/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String nombre,
                         @RequestParam(required = false) String confirmar,
                         @RequestParam(defaultValue = "false") boolean incluirDlq) {
        service.eliminar(nombre, confirmar, incluirDlq);
    }
}
