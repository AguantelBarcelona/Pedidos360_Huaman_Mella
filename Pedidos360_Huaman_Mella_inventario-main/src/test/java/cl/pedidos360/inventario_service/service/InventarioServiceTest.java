package cl.pedidos360.inventario_service.service;

import cl.pedidos360.inventario_service.model.Inventario;
import cl.pedidos360.inventario_service.repository.InventarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class InventarioServiceTest {

    @Mock
    private InventarioRepository inventarioRepository;

    private InventarioService inventarioService;

    @BeforeEach
    void setUp() {
        inventarioService = new InventarioService(
                inventarioRepository
        );
    }

    @Test
    void listarTodosDebeRetornarInventariosDelRepositorio() {

        Inventario inventario = new Inventario(
                1L,
                10L,
                20,
                5
        );

        List<Inventario> inventariosEsperados =
                List.of(inventario);

        when(inventarioRepository.findAll())
                .thenReturn(inventariosEsperados);

        List<Inventario> resultado =
                inventarioService.listarTodos();

        assertEquals(
                1,
                resultado.size()
        );

        assertSame(
                inventario,
                resultado.get(0)
        );

        verify(inventarioRepository)
                .findAll();
    }

    @Test
    void buscarPorProductoDebeRetornarInventarioCuandoExiste() {

        Inventario inventario = new Inventario(
                1L,
                10L,
                20,
                5
        );

        when(inventarioRepository.findByProductoId(10L))
                .thenReturn(
                        Optional.of(inventario)
                );

        Inventario resultado =
                inventarioService.buscarPorProducto(10L);

        assertSame(
                inventario,
                resultado
        );

        verify(inventarioRepository)
                .findByProductoId(10L);
    }

    @Test
    void buscarPorProductoDebeLanzarExcepcionCuandoNoExiste() {

        when(inventarioRepository.findByProductoId(99L))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                InventarioNoEncontradoException.class,
                () -> inventarioService.buscarPorProducto(99L)
        );

        verify(inventarioRepository)
                .findByProductoId(99L);
    }

    @Test
    void crearDebeEliminarIdYAsignarStockReservadoCeroCuandoEsNull() {

        Inventario inventario = new Inventario(
                999L,
                10L,
                15,
                null
        );

        when(inventarioRepository.save(any(Inventario.class)))
                .thenAnswer(
                        invocacion ->
                                invocacion.getArgument(0)
                );

        inventarioService.crear(inventario);

        ArgumentCaptor<Inventario> captor =
                ArgumentCaptor.forClass(
                        Inventario.class
                );

        verify(inventarioRepository)
                .save(captor.capture());

        Inventario guardado =
                captor.getValue();

        assertNull(
                guardado.getId()
        );

        assertEquals(
                10L,
                guardado.getProductoId().longValue()
        );

        assertEquals(
                15,
                guardado.getStockDisponible().intValue()
        );

        assertEquals(
                0,
                guardado.getStockReservado().intValue()
        );
    }

    @Test
    void actualizarStockDebeModificarStockDisponible() {

        Inventario inventario = new Inventario(
                1L,
                10L,
                8,
                2
        );

        when(inventarioRepository.findByProductoId(10L))
                .thenReturn(
                        Optional.of(inventario)
                );

        when(inventarioRepository.save(inventario))
                .thenReturn(inventario);

        Inventario resultado =
                inventarioService.actualizarStock(
                        10L,
                        25
                );

        assertEquals(
                25,
                resultado.getStockDisponible().intValue()
        );

        assertEquals(
                2,
                resultado.getStockReservado().intValue()
        );

        verify(inventarioRepository)
                .save(inventario);
    }

    @Test
    void reservarStockDebeMoverCantidadDeDisponibleAReservado() {

        Inventario inventario = new Inventario(
                1L,
                10L,
                10,
                2
        );

        when(inventarioRepository.findByProductoId(10L))
                .thenReturn(
                        Optional.of(inventario)
                );

        when(inventarioRepository.save(inventario))
                .thenReturn(inventario);

        Inventario resultado =
                inventarioService.reservarStock(
                        10L,
                        3
                );

        assertEquals(
                7,
                resultado.getStockDisponible().intValue()
        );

        assertEquals(
                5,
                resultado.getStockReservado().intValue()
        );

        verify(inventarioRepository)
                .save(inventario);
    }

    @Test
    void reservarStockDebeLanzarExcepcionCuandoNoHayStockSuficiente() {

        Inventario inventario = new Inventario(
                1L,
                10L,
                2,
                1
        );

        when(inventarioRepository.findByProductoId(10L))
                .thenReturn(
                        Optional.of(inventario)
                );

        assertThrows(
                StockInsuficienteException.class,
                () -> inventarioService.reservarStock(
                        10L,
                        5
                )
        );

        assertEquals(
                2,
                inventario.getStockDisponible().intValue()
        );

        assertEquals(
                1,
                inventario.getStockReservado().intValue()
        );
    }

    @Test
    void liberarStockDebeMoverCantidadDeReservadoADisponible() {

        Inventario inventario = new Inventario(
                1L,
                10L,
                7,
                5
        );

        when(inventarioRepository.findByProductoId(10L))
                .thenReturn(
                        Optional.of(inventario)
                );

        when(inventarioRepository.save(inventario))
                .thenReturn(inventario);

        Inventario resultado =
                inventarioService.liberarStock(
                        10L,
                        2
                );

        assertEquals(
                9,
                resultado.getStockDisponible().intValue()
        );

        assertEquals(
                3,
                resultado.getStockReservado().intValue()
        );

        verify(inventarioRepository)
                .save(inventario);
    }

    @Test
    void liberarStockDebeLanzarExcepcionCuandoNoHayStockReservadoSuficiente() {

        Inventario inventario = new Inventario(
                1L,
                10L,
                8,
                1
        );

        when(inventarioRepository.findByProductoId(10L))
                .thenReturn(
                        Optional.of(inventario)
                );

        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> inventarioService.liberarStock(
                                10L,
                                3
                        )
                );

        assertEquals(
                "No existe suficiente stock reservado",
                excepcion.getMessage()
        );

        assertEquals(
                8,
                inventario.getStockDisponible().intValue()
        );

        assertEquals(
                1,
                inventario.getStockReservado().intValue()
        );
    }
}