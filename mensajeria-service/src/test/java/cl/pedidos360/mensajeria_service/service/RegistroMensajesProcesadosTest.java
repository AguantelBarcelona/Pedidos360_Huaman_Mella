package cl.pedidos360.mensajeria_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegistroMensajesProcesadosTest {

    @Test
    void debeRegistrarUnEventoNuevo() {

        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        when(jdbc.update(
                anyString(),
                eq("evento-18"),
                eq("notificaciones"),
                eq("PEDIDO_CREADO"),
                eq("mensaje-1")
        )).thenReturn(1);

        RegistroMensajesProcesados registro =
                new RegistroMensajesProcesados(jdbc);

        boolean resultado = registro.registrarSiNuevo(
                "evento-18",
                "notificaciones",
                "PEDIDO_CREADO",
                "mensaje-1"
        );

        assertTrue(resultado);
    }

    @Test
    void debeReconocerUnEventoDuplicado() {

        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        when(jdbc.update(
                anyString(),
                eq("evento-18"),
                eq("notificaciones"),
                eq("PEDIDO_CREADO"),
                eq("mensaje-1")
        )).thenReturn(1, 0);

        RegistroMensajesProcesados registro =
                new RegistroMensajesProcesados(jdbc);

        assertTrue(registro.registrarSiNuevo(
                "evento-18",
                "notificaciones",
                "PEDIDO_CREADO",
                "mensaje-1"
        ));

        assertFalse(registro.registrarSiNuevo(
                "evento-18",
                "notificaciones",
                "PEDIDO_CREADO",
                "mensaje-1"
        ));
    }

    @Test
    void debeRechazarUnEventIdVacio() {

        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        RegistroMensajesProcesados registro =
                new RegistroMensajesProcesados(jdbc);

        assertThrows(
                IllegalArgumentException.class,
                () -> registro.registrarSiNuevo(
                        "",
                        "notificaciones",
                        "PEDIDO_CREADO",
                        "mensaje-1"
                )
        );

        verifyNoInteractions(jdbc);
    }
}
