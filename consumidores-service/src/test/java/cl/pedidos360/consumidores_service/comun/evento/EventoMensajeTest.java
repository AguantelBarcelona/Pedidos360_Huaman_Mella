package cl.pedidos360.consumidores_service.comun.evento;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class EventoMensajeTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void leeElFormatoDePedidosService() {
        EventoMensaje e = EventoMensaje.desdeJson("""
                {"eventId":"abc","eventType":"PEDIDO_CREADO","pedidoId":7,"productoId":3,
                 "cantidad":4,"clienteEmail":"a@b.cl","origen":"pedidos-service","fechaEvento":"x"}""",
                null, json);
        assertEquals("abc", e.eventId());
        assertEquals("PEDIDO_CREADO", e.eventType());
        assertEquals(7L, e.pedidoId());
        assertEquals(3L, e.productoId());
        assertEquals(4, e.cantidad());
        assertEquals("a@b.cl", e.clienteEmail());
    }

    @Test
    void usaAtributoSiElCuerpoNoTraeEventType() {
        EventoMensaje e = EventoMensaje.desdeJson("{\"eventId\":\"x1\",\"productoId\":5}", "PRODUCTO_ACTUALIZADO", json);
        assertEquals("PRODUCTO_ACTUALIZADO", e.eventType());
    }

    @Test
    void desenvuelveSobreSnsSinRawDelivery() {
        String sobre = "{\"Type\":\"Notification\",\"Message\":\"{\\\"eventId\\\":\\\"z\\\",\\\"eventType\\\":\\\"PEDIDO_CREADO\\\"}\"}";
        assertEquals("z", EventoMensaje.desdeJson(sobre, null, json).eventId());
    }

    @Test
    void sinEventIdEsDefinitivo() {
        assertThrows(ErrorDefinitivoException.class,
                () -> EventoMensaje.desdeJson("{\"eventType\":\"PEDIDO_CREADO\"}", null, json));
    }

    @Test
    void jsonRotoEsDefinitivo() {
        assertThrows(ErrorDefinitivoException.class, () -> EventoMensaje.desdeJson("no-json", null, json));
    }
}
