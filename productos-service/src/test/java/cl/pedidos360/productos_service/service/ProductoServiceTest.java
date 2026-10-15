package cl.pedidos360.productos_service.service;

import cl.pedidos360.productos_service.model.Producto;
import cl.pedidos360.productos_service.repository.ProductoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    private ProductoService productoService;

    @BeforeEach
    void setUp() {
        productoService = new ProductoService(
                productoRepository
        );
    }

    @Test
    void listarTodosDebeRetornarProductosDelRepositorio() {

        Producto producto = crearProducto(
                1L,
                "Air Max",
                "Nike",
                42,
                "45990",
                "Calzado",
                "Producto de prueba"
        );

        List<Producto> productosEsperados =
                List.of(producto);

        when(productoRepository.findAll())
                .thenReturn(productosEsperados);

        List<Producto> resultado =
                productoService.listarTodos();

        assertEquals(
                1,
                resultado.size()
        );

        assertSame(
                producto,
                resultado.get(0)
        );

        verify(productoRepository)
                .findAll();
    }

    @Test
    void buscarPorIdDebeRetornarProductoCuandoExiste() {

        Producto producto = crearProducto(
                1L,
                "Air Max",
                "Nike",
                42,
                "45990",
                "Calzado",
                "Producto de prueba"
        );

        when(productoRepository.findById(1L))
                .thenReturn(
                        Optional.of(producto)
                );

        Producto resultado =
                productoService.buscarPorId(1L);

        assertSame(
                producto,
                resultado
        );

        verify(productoRepository)
                .findById(1L);
    }

    @Test
    void buscarPorIdDebeLanzarExcepcionCuandoNoExiste() {

        when(productoRepository.findById(99L))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ProductoNoEncontradoException.class,
                () -> productoService.buscarPorId(99L)
        );

        verify(productoRepository)
                .findById(99L);
    }

    @Test
    void crearDebeEliminarIdRecibidoAntesDeGuardar() {

        Producto producto = crearProducto(
                999L,
                "Air Max",
                "Nike",
                42,
                "45990",
                "Calzado",
                "Producto nuevo"
        );

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(
                        invocacion ->
                                invocacion.getArgument(0)
                );

        productoService.crear(producto);

        ArgumentCaptor<Producto> captor =
                ArgumentCaptor.forClass(
                        Producto.class
                );

        verify(productoRepository)
                .save(captor.capture());

        Producto guardado =
                captor.getValue();

        assertNull(
                guardado.getId()
        );

        assertEquals(
                "Air Max",
                guardado.getModelo()
        );

        assertEquals(
                "Nike",
                guardado.getMarca()
        );
    }

    @Test
    void actualizarDebeModificarLosDatosDelProductoExistente() {

        Producto existente = crearProducto(
                1L,
                "Modelo antiguo",
                "Marca antigua",
                40,
                "10000",
                "Categoria antigua",
                "Descripcion antigua"
        );

        Producto nuevosDatos = crearProducto(
                null,
                "Modelo actualizado",
                "Nike",
                43,
                "59990",
                "Running",
                "Descripcion actualizada"
        );

        when(productoRepository.findById(1L))
                .thenReturn(
                        Optional.of(existente)
                );

        when(productoRepository.save(existente))
                .thenReturn(existente);

        Producto resultado =
                productoService.actualizar(
                        1L,
                        nuevosDatos
                );

        assertEquals(
                1L,
                resultado.getId().longValue()
        );

        assertEquals(
                "Modelo actualizado",
                resultado.getModelo()
        );

        assertEquals(
                "Nike",
                resultado.getMarca()
        );

        assertEquals(
                43,
                resultado.getTalla().intValue()
        );

        assertEquals(
                0,
                new BigDecimal("59990")
                        .compareTo(
                                resultado.getPrecio()
                        )
        );

        assertEquals(
                "Running",
                resultado.getCategoria()
        );

        assertEquals(
                "Descripcion actualizada",
                resultado.getDescripcion()
        );

        verify(productoRepository)
                .save(existente);
    }

    @Test
    void eliminarDebeBuscarProductoYEliminarlo() {

        Producto producto = crearProducto(
                1L,
                "Air Max",
                "Nike",
                42,
                "45990",
                "Calzado",
                "Producto de prueba"
        );

        when(productoRepository.findById(1L))
                .thenReturn(
                        Optional.of(producto)
                );

        productoService.eliminar(1L);

        verify(productoRepository)
                .findById(1L);

        verify(productoRepository)
                .delete(producto);
    }

    private Producto crearProducto(
            Long id,
            String modelo,
            String marca,
            Integer talla,
            String precio,
            String categoria,
            String descripcion
    ) {
        return new Producto(
                id,
                modelo,
                marca,
                talla,
                new BigDecimal(precio),
                categoria,
                descripcion
        );
    }
}