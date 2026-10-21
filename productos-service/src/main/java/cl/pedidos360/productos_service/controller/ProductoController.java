package cl.pedidos360.productos_service.controller;

import cl.pedidos360.productos_service.model.Producto;
import cl.pedidos360.productos_service.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }


    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        return ResponseEntity.ok(productoService.listarTodos());
    }


    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }


    @GetMapping("/marca/{marca}")
    public ResponseEntity<List<Producto>> porMarca(
            @PathVariable String marca) {

        return ResponseEntity.ok(
                productoService.buscarPorMarca(marca)
        );
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Producto>> porCategoria(
            @PathVariable String categoria) {

        return ResponseEntity.ok(
                productoService.buscarPorCategoria(categoria)
        );
    }

    @PostMapping
    public ResponseEntity<Producto> crear(
            @Valid @RequestBody Producto producto) {

        Producto nuevoProducto = productoService.crear(producto);

        URI ubicacion = URI.create(
                "/api/productos/" + nuevoProducto.getId()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(nuevoProducto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Producto producto) {

        return ResponseEntity.ok(
                productoService.actualizar(id, producto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        productoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}