package cl.pedidos360.consumidores_service.notificaciones;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionProcesadorTest {

    private final NotificacionRepository repo = mock(NotificacionRepository.class);
    private final NotificacionProcesador procesador = new NotificacionProcesador(repo);

    private static EventoMensaje evento(String tipo, String email) {
        return new EventoMensaje("e1", tipo, 10L, 1L, 2, email, "pedidos-service", "f", "{}");
    }

    @Test
    void pedidoCreadoGuardaNotificacion() {
        procesador.procesar(evento("PEDIDO_CREADO", "c@duoc.cl"));
        ArgumentCaptor<Notificacion> n = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(n.capture());
        assertEquals("c@duoc.cl", n.getValue().getDestinatario());
        assertTrue(n.getValue().getAsunto().contains("#10"));
    }

    @Test
    void sinEmailEsDefinitivo() {
        assertThrows(ErrorDefinitivoException.class, () -> procesador.procesar(evento("PEDIDO_CREADO", null)));
        verifyNoInteractions(repo);
    }

    @Test
    void tipoNoSoportadoEsDefinitivo() {
        assertThrows(ErrorDefinitivoException.class, () -> procesador.procesar(evento("PRODUCTO_ACTUALIZADO", "c@duoc.cl")));
    }
}
