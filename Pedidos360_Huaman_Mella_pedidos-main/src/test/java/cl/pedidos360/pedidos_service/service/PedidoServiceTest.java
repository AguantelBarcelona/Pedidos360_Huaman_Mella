package cl.pedidos360.pedidos_service.service;

import cl.pedidos360.pedidos_service.client.InventarioClient;
import cl.pedidos360.pedidos_service.client.ProductoClient;
import cl.pedidos360.pedidos_service.client.ProductoResponse;
import cl.pedidos360.pedidos_service.dto.CrearPedidoRequest;
import cl.pedidos360.pedidos_service.model.Pedido;
import cl.pedidos360.pedidos_service.repository.PedidoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoClient productoClient;

    @Mock
    private InventarioClient inventarioClient;

    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService(
                pedidoRepository,
                productoClient,
                inventarioClient
        );
    }

    @Test
    void crearDebeUsarSubPrecioOficialReservarStockYGuardar() {

        CrearPedidoRequest request = new CrearPedidoRequest(
                "cliente@duoc.cl",
                1L,
                2
        );

        ProductoResponse producto = new ProductoResponse(
                1L,
                "Air Max",
                "Nike",
                42,
                new BigDecimal("45990"),
                "Calzado",
                "Producto de prueba"
        );

        when(productoClient.obtenerProducto(1L))
                .thenReturn(producto);

        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacion -> {
                    Pedido pedido = invocacion.getArgument(0);

                    /*
                     * En una persistencia real JPA ejecuta @PrePersist.
                     * Como el repositorio está mockeado, se ejecuta
                     * manualmente para representar ese comportamiento.
                     */
                    pedido.alCrear();
                    pedido.setId(10L);

                    return pedido;
                });

        Pedido resultado = pedidoService.crear(
                request,
                "sub-cognito-123"
        );

        assertNotNull(resultado);

        assertEquals(
                10L,
                resultado.getId().longValue()
        );

        assertEquals(
                "cliente@duoc.cl",
                resultado.getClienteEmail()
        );

        assertEquals(
                "sub-cognito-123",
                resultado.getClienteSub()
        );

        assertEquals(
                1L,
                resultado.getProductoId().longValue()
        );

        assertEquals(
                2,
                resultado.getCantidad().intValue()
        );

        assertEquals(
                0,
                new BigDecimal("45990").compareTo(
                        resultado.getPrecioUnitario()
                )
        );

        assertEquals(
                0,
                new BigDecimal("91980").compareTo(
                        resultado.getTotal()
                )
        );

        assertEquals(
                "CREADO",
                resultado.getEstado()
        );

        assertNotNull(
                resultado.getFechaCreacion()
        );

        verify(productoClient)
                .obtenerProducto(1L);

        verify(inventarioClient)
                .reservarStock(
                        1L,
                        2
                );

        verify(pedidoRepository)
                .save(any(Pedido.class));
    }

    @Test
    void crearDebeGuardarElPrecioObtenidoDesdeProductosService() {

        CrearPedidoRequest request = new CrearPedidoRequest(
                "cliente@duoc.cl",
                5L,
                1
        );

        ProductoResponse producto = new ProductoResponse(
                5L,
                "Producto Test",
                "Marca Test",
                40,
                new BigDecimal("35000"),
                "Calzado",
                null
        );

        when(productoClient.obtenerProducto(5L))
                .thenReturn(producto);

        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        pedidoService.crear(
                request,
                "sub-usuario"
        );

        ArgumentCaptor<Pedido> captor =
                ArgumentCaptor.forClass(Pedido.class);

        verify(pedidoRepository)
                .save(captor.capture());

        Pedido guardado = captor.getValue();

        assertEquals(
                0,
                new BigDecimal("35000").compareTo(
                        guardado.getPrecioUnitario()
                )
        );
    }

    @Test
    void buscarPorClienteSubDebeConsultarRepositorioPorSub() {

        Pedido pedido = new Pedido();
        pedido.setId(20L);
        pedido.setClienteSub(
                "sub-cliente-456"
        );

        List<Pedido> pedidosEsperados =
                List.of(pedido);

        when(
                pedidoRepository.findByClienteSub(
                        "sub-cliente-456"
                )
        ).thenReturn(pedidosEsperados);

        List<Pedido> resultado =
                pedidoService.buscarPorClienteSub(
                        "sub-cliente-456"
                );

        assertEquals(
                1,
                resultado.size()
        );

        assertEquals(
                20L,
                resultado.get(0).getId().longValue()
        );

        assertEquals(
                "sub-cliente-456",
                resultado.get(0).getClienteSub()
        );

        verify(pedidoRepository)
                .findByClienteSub(
                        "sub-cliente-456"
                );
    }
}