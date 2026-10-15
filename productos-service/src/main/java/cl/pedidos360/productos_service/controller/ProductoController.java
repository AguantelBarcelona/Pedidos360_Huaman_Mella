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

    /**
     * Obtiene todos los productos disponibles en el catálogo.
     */
    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

    /**
     * Obtiene un producto específico mediante su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    /**
     * Filtra productos por marca.
     */
    @GetMapping("/marca/{marca}")
    public ResponseEntity<List<Producto>> porMarca(
            @PathVariable String marca) {

        return ResponseEntity.ok(
                productoService.buscarPorMarca(marca)
        );
    }

    /**
     * Filtra productos por categoría.
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Producto>> porCategoria(
            @PathVariable String categoria) {

        return ResponseEntity.ok(
                productoService.buscarPorCategoria(categoria)
        );
    }

    /**
     * Crea un nuevo producto.
     */
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

    /**
     * Actualiza completamente un producto existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Producto producto) {

        return ResponseEntity.ok(
                productoService.actualizar(id, producto)
        );
    }

    /**
     * Elimina un producto existente.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        productoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}