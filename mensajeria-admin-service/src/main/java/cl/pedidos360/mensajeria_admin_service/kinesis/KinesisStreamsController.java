package cl.pedidos360.mensajeria_admin_service.kinesis;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kinesis/streams")
@PreAuthorize("hasRole('ADMIN')")
public class KinesisStreamsController {

    private final KinesisStreamService servicio;

    public KinesisStreamsController(
            KinesisStreamService servicio) {

        this.servicio = servicio;
    }

    public record CrearStreamRequest(
            String nombre,
            Integer shards
    ) {}

    public record ShardsRequest(
            Integer shards
    ) {}

    public record RetencionRequest(
            Integer horas
    ) {}

    @GetMapping
    public List<String> listar() {
        return servicio.listar();
    }

    @GetMapping("/{nombre}")
    public KinesisStreamService.StreamInfo detalle(
            @PathVariable String nombre) {

        return servicio.detalle(nombre);
    }

    @PostMapping
    public ResponseEntity<KinesisStreamService.Operacion> crear(
            @RequestBody CrearStreamRequest request) {

        return ResponseEntity.accepted().body(
                servicio.crear(
                        request.nombre(),
                        request.shards()
                )
        );
    }

    @PutMapping("/{nombre}/shards")
    public ResponseEntity<KinesisStreamService.Operacion> actualizarShards(
            @PathVariable String nombre,
            @RequestBody ShardsRequest request) {

        return ResponseEntity.accepted().body(
                servicio.actualizarShards(
                        nombre,
                        request.shards()
                )
        );
    }

    @PutMapping("/{nombre}/retention")
    public ResponseEntity<KinesisStreamService.Operacion> actualizarRetencion(
            @PathVariable String nombre,
            @RequestBody RetencionRequest request,
            @RequestParam(required = false) String confirmar) {

        return ResponseEntity.accepted().body(
                servicio.actualizarRetencion(
                        nombre,
                        request.horas(),
                        confirmar
                )
        );
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<KinesisStreamService.Operacion> eliminar(
            @PathVariable String nombre,
            @RequestParam(required = false) String confirmar) {

        return ResponseEntity.accepted().body(
                servicio.eliminar(
                        nombre,
                        confirmar
                )
        );
    }
}
