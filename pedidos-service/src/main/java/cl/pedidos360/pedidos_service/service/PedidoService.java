package cl.pedidos360.pedidos_service.service;

import cl.pedidos360.pedidos_service.client.InventarioClient;
import cl.pedidos360.pedidos_service.client.ProductoClient;
import cl.pedidos360.pedidos_service.client.ProductoResponse;
import cl.pedidos360.pedidos_service.dto.CrearPedidoRequest;
import cl.pedidos360.pedidos_service.model.Pedido;
import cl.pedidos360.pedidos_service.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final InventarioClient inventarioClient;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoClient productoClient,
            InventarioClient inventarioClient) {

        this.pedidoRepository = pedidoRepository;
        this.productoClient = productoClient;
        this.inventarioClient = inventarioClient;
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    public List<Pedido> buscarPorCliente(String email) {
        return pedidoRepository.findByClienteEmail(email);
    }

    public List<Pedido> buscarPorClienteSub(String clienteSub) {
        return pedidoRepository.findByClienteSub(clienteSub);
    }

    public Pedido crear(
            CrearPedidoRequest request,
            String clienteSub) {

        Pedido pedido = new Pedido();

        // Datos proporcionados por la solicitud.
        pedido.setClienteEmail(request.clienteEmail());
        pedido.setProductoId(request.productoId());
        pedido.setCantidad(request.cantidad());

        // La identidad real del propietario proviene del JWT.
        pedido.setClienteSub(clienteSub);

        // El estado inicial lo define el backend.
        pedido.setEstado("CREADO");

        // Consultar productos-service para obtener el precio oficial.
        ProductoResponse producto =
                productoClient.obtenerProducto(
                        request.productoId()
                );

        pedido.setPrecioUnitario(producto.precio());

        // Reservar stock antes de guardar el pedido.
        inventarioClient.reservarStock(
                request.productoId(),
                request.cantidad()
        );

        // @PrePersist calculará total y fechaCreacion.
        return pedidoRepository.save(pedido);
    }

    public Pedido actualizar(Long id, Pedido datos) {

        Pedido existente = buscarPorId(id);

        // Consultar nuevamente el precio oficial del producto.
        ProductoResponse producto =
                productoClient.obtenerProducto(
                        datos.getProductoId()
                );

        existente.setClienteEmail(datos.getClienteEmail());
        existente.setProductoId(datos.getProductoId());
        existente.setCantidad(datos.getCantidad());
        existente.setPrecioUnitario(producto.precio());

        return pedidoRepository.save(existente);
    }

    public void eliminar(Long id) {

        Pedido pedido = buscarPorId(id);

        pedidoRepository.delete(pedido);
    }
}