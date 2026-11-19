package cl.pedidos360.consumidores_kinesis_service.procesamiento;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcesadorEventosTest {

    private JdbcTemplate jdbc;
    private ProcesadorEventos procesador;

    private static final String APP = "p360-pedidos-analitica";

    private static final UUID EVENT_ID =
            UUID.fromString("4f2b20f4-e50e-46d2-b298-8f0ecbc811e0");

    private static final String PEDIDO = """
        {
          "eventId":"4f2b20f4-e50e-46d2-b298-8f0ecbc811e0",
          "eventType":"PEDIDO_CREADO",
          "pedidoId":37,
          "productoId":11,
          "cantidad":1,
          "total":10000,
          "estado":"CREADO",
          "origen":"pedidos-service",
          "fechaEvento":"2026-11-16T15:26:44Z"
        }
        """;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        procesador = new ProcesadorEventos(jdbc);
    }

    @Test
    void debeProcesarPedidoNuevo() {

        when(jdbc.update(
                contains("kinesis_eventos_procesados"),
                eq(APP),
                eq(EVENT_ID),
                eq("PEDIDO_CREADO")
        )).thenReturn(1);

        var resultado = procesador.procesar(
                APP,
                DestinoConsumidor.PEDIDOS_ANALITICA,
                "37",
                PEDIDO
        );

        assertTrue(resultado.nuevo());
        assertEquals(EVENT_ID.toString(), resultado.eventId());

        verify(jdbc).update(
                contains("kinesis_pedidos_analitica"),
                eq(EVENT_ID),
                eq(37L),
                eq(11L),
                eq(1),
                eq(new BigDecimal("10000")),
                any(OffsetDateTime.class)
        );
    }

    @Test
    void debeOmitirEventoDuplicado() {

        when(jdbc.update(
                contains("kinesis_eventos_procesados"),
                eq(APP),
                eq(EVENT_ID),
                eq("PEDIDO_CREADO")
        )).thenReturn(0);

        var resultado = procesador.procesar(
                APP,
                DestinoConsumidor.PEDIDOS_ANALITICA,
                "37",
                PEDIDO
        );

        assertFalse(resultado.nuevo());

        verify(jdbc).update(
                contains("kinesis_eventos_procesados"),
                eq(APP),
                eq(EVENT_ID),
                eq("PEDIDO_CREADO")
        );

        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void debeRechazarPartitionKeyIncorrecta() {

        assertThrows(IllegalArgumentException.class, () ->
                procesador.procesar(
                        APP,
                        DestinoConsumidor.PEDIDOS_ANALITICA,
                        "999",
                        PEDIDO
                )
        );

        verifyNoInteractions(jdbc);
    }

    @Test
    void debeRechazarCantidadNegativa() {

        String invalido = PEDIDO.replace(
                "\"cantidad\":1",
                "\"cantidad\":-1"
        );

        assertThrows(IllegalArgumentException.class, () ->
                procesador.procesar(
                        APP,
                        DestinoConsumidor.PEDIDOS_ANALITICA,
                        "37",
                        invalido
                )
        );

        verifyNoInteractions(jdbc);
    }

    @Test
    void debeProcesarEventoDeInventario() {

        String app = "p360-inventario-proyeccion";

        UUID id = UUID.fromString(
                "faaeda69-6afd-4798-82c8-ba6c5e696f0b");

        String inventario = """
            {
              "eventId":"faaeda69-6afd-4798-82c8-ba6c5e696f0b",
              "eventType":"STOCK_RESERVADO",
              "productoId":11,
              "stockDisponible":1,
              "stockReservado":9,
              "origen":"inventario-service",
              "fechaEvento":"2026-11-16T15:26:44Z"
            }
            """;

        when(jdbc.update(
                contains("kinesis_eventos_procesados"),
                eq(app),
                eq(id),
                eq("STOCK_RESERVADO")
        )).thenReturn(1);

        var resultado = procesador.procesar(
                app,
                DestinoConsumidor.INVENTARIO_PROYECCION,
                "11",
                inventario
        );

        assertTrue(resultado.nuevo());

        verify(jdbc).update(
                contains("kinesis_inventario_proyeccion"),
                eq(11L),
                eq(id),
                eq("STOCK_RESERVADO"),
                eq(1),
                eq(9),
                any(OffsetDateTime.class)
        );
    }
}
