package cl.pedidos360.consumidores_service.controller;

import cl.pedidos360.consumidores_service.comun.sqs.ConsumidorSqs;
import cl.pedidos360.consumidores_service.comun.idempotencia.EventoProcesado;
import cl.pedidos360.consumidores_service.comun.idempotencia.EventoProcesadoRepository;
import cl.pedidos360.consumidores_service.comun.sqs.RegistroConsumidores;
import cl.pedidos360.consumidores_service.comun.fallos.RegistroFallo;
import cl.pedidos360.consumidores_service.comun.fallos.RegistroFalloRepository;
import cl.pedidos360.consumidores_service.comun.fallos.SimuladorFallos;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Operacion de los consumidores de ESTA instancia. Requiere grupo ADMIN (ver SecurityConfig).
 * mensajeria-admin-service usa estos endpoints para pausar/reanudar.
 */
@RestController
@RequestMapping("/api/consumidores")
public class ConsumidorController {

    private final RegistroConsumidores registro;
    private final SimuladorFallos simulador;
    private final RegistroFalloRepository fallos;
    private final EventoProcesadoRepository procesados;

    public ConsumidorController(RegistroConsumidores registro, SimuladorFallos simulador,
                                RegistroFalloRepository fallos, EventoProcesadoRepository procesados) {
        this.registro = registro;
        this.simulador = simulador;
        this.fallos = fallos;
        this.procesados = procesados;
    }

    @GetMapping
    public List<ConsumidorSqs.EstadoConsumidor> estado() {
        return registro.todos().stream().map(ConsumidorSqs::estado).toList();
    }

    @GetMapping("/{dominio}")
    public ConsumidorSqs.EstadoConsumidor estado(@PathVariable String dominio) {
        return buscar(dominio).estado();
    }

    @PostMapping("/{dominio}/pausar")
    public ConsumidorSqs.EstadoConsumidor pausar(@PathVariable String dominio) {
        ConsumidorSqs c = buscar(dominio);
        c.pausar();
        return c.estado();
    }

    @PostMapping("/{dominio}/reanudar")
    public ConsumidorSqs.EstadoConsumidor reanudar(@PathVariable String dominio) {
        ConsumidorSqs c = buscar(dominio);
        c.reanudar();
        return c.estado();
    }

    /** Solo demo. tipo = NINGUNO | RECUPERABLE | DEFINITIVO */
    @PostMapping("/{dominio}/simular-fallo")
    public ConsumidorSqs.EstadoConsumidor simularFallo(@PathVariable String dominio,
                                                       @RequestParam(defaultValue = "NINGUNO") String tipo) {
        ConsumidorSqs c = buscar(dominio);
        try {
            simulador.configurar(dominio, SimuladorFallos.Modo.valueOf(tipo.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo debe ser NINGUNO, RECUPERABLE o DEFINITIVO");
        }
        return c.estado();
    }

    @GetMapping("/fallos")
    public List<RegistroFallo> fallos(@RequestParam(required = false) String dominio) {
        return dominio == null ? fallos.findTop100ByOrderByFechaDesc()
                : fallos.findTop100ByDominioOrderByFechaDesc(dominio);
    }

    @GetMapping("/procesados")
    public List<EventoProcesado> procesados(@RequestParam(required = false) String dominio) {
        return dominio == null ? procesados.findTop50ByOrderByProcesadoEnDesc()
                : procesados.findTop50ByDominioOrderByProcesadoEnDesc(dominio);
    }

    private ConsumidorSqs buscar(String dominio) {
        return registro.obtener(dominio).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Consumidor '" + dominio + "' no esta activo en esta instancia"));
    }
}
