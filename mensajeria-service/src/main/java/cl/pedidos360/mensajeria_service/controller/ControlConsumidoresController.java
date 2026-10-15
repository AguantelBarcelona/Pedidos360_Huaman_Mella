package cl.pedidos360.mensajeria_service.controller;

import cl.pedidos360.mensajeria_service.service.ControlConsumidores;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/mensajeria/consumidores")
public class ControlConsumidoresController {

    private final ControlConsumidores control;

    public ControlConsumidoresController(ControlConsumidores control) {
        this.control = control;
    }

    // Consultar el estado de los tres consumidores.
    @GetMapping
    public Map<String, Boolean> listarEstados() {
        return control.obtenerEstados();
    }

    // Pausar un consumidor.
    @PostMapping("/{nombre}/pausar")
    public Map<String, Object> pausar(@PathVariable String nombre) {

        try {
            control.pausar(nombre);

            return Map.of(
                    "consumidor", nombre,
                    "activo", false,
                    "mensaje", "Consumidor pausado"
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // Reanudar un consumidor.
    @PostMapping("/{nombre}/reanudar")
    public Map<String, Object> reanudar(@PathVariable String nombre) {

        try {
            control.reanudar(nombre);

            return Map.of(
                    "consumidor", nombre,
                    "activo", true,
                    "mensaje", "Consumidor reanudado"
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }
}
