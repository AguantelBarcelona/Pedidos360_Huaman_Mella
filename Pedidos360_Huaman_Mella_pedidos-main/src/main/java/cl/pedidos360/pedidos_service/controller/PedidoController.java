package cl.pedidos360.pedidos_service.controller;

import cl.pedidos360.pedidos_service.dto.CrearPedidoRequest;
import cl.pedidos360.pedidos_service.model.Pedido;
import cl.pedidos360.pedidos_service.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Pedido> listar() {
        return pedidoService.listarTodos();
    }

    @GetMapping("/mios")
    public List<Pedido> listarPedidosDelUsuario(
            @AuthenticationPrincipal Jwt jwt) {

        return pedidoService.buscarPorClienteSub(
                jwt.getSubject()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Pedido obtener(
            @PathVariable Long id) {

        return pedidoService.buscarPorId(id);
    }

    @GetMapping("/cliente/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Pedido> buscarPorCliente(
            @PathVariable String email) {

        return pedidoService.buscarPorCliente(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(
            @Valid @RequestBody CrearPedidoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return pedidoService.crear(
                request,
                jwt.getSubject()
        );
    }

    @PutMapping("/{id}")
    public Pedido actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Pedido pedido) {

        return pedidoService.actualizar(
                id,
                pedido
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long id) {

        pedidoService.eliminar(id);
    }
}