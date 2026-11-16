package cl.pedidos360.mensajeria_admin_service.kinesis;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kinesis/aplicaciones")
@PreAuthorize("hasRole('ADMIN')")
public class KinesisMonitoreoController {

    private final KinesisMonitoreoService servicio;

    public KinesisMonitoreoController(
            KinesisMonitoreoService servicio) {

        this.servicio = servicio;
    }

    @GetMapping
    public List<KinesisMonitoreoService.Aplicacion> listar() {

        return servicio.listar();
    }

    @GetMapping("/{nombre}/checkpoints")
    public KinesisMonitoreoService.CheckpointsInfo checkpoints(
            @PathVariable String nombre) {

        return servicio.checkpoints(nombre);
    }

    @GetMapping("/{nombre}/lag")
    public KinesisMonitoreoService.LagInfo lag(
            @PathVariable String nombre,
            @RequestParam(required = false) String shard) {

        return servicio.lag(nombre, shard);
    }
}
