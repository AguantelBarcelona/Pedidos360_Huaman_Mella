package cl.pedidos360.inventario_service.controller;

import cl.pedidos360.inventario_service.model.Inventario;
import cl.pedidos360.inventario_service.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public ResponseEntity<List<Inventario>> listar() {
        return ResponseEntity.ok(inventarioService.listarTodos());
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<Inventario> obtenerPorProducto(
            @PathVariable Long productoId) {

        return ResponseEntity.ok(
                inventarioService.buscarPorProducto(productoId)
        );
    }

    @PostMapping
    public ResponseEntity<Inventario> crear(
            @Valid @RequestBody Inventario inventario) {

        Inventario nuevoInventario =
                inventarioService.crear(inventario);

        URI ubicacion = URI.create(
                "/api/inventario/producto/"
                        + nuevoInventario.getProductoId()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(nuevoInventario);
    }

    @PutMapping("/producto/{productoId}")
    public ResponseEntity<Inventario> actualizarStock(
            @PathVariable Long productoId,
            @RequestParam Integer stock) {

        return ResponseEntity.ok(
                inventarioService.actualizarStock(
                        productoId,
                        stock
                )
        );
    }

    @PostMapping("/producto/{productoId}/reservar")
    public ResponseEntity<Inventario> reservarStock(
            @PathVariable Long productoId,
            @RequestParam Integer cantidad) {

        return ResponseEntity.ok(
                inventarioService.reservarStock(
                        productoId,
                        cantidad
                )
        );
    }

    @PostMapping("/producto/{productoId}/liberar")
    public ResponseEntity<Inventario> liberarStock(
            @PathVariable Long productoId,
            @RequestParam Integer cantidad) {

        return ResponseEntity.ok(
                inventarioService.liberarStock(
                        productoId,
                        cantidad
                )
        );
    }
}