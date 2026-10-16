package cl.pedidos360.consumidores_service.documentos;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentoProcesadorTest {

    private final DocumentoRepository repo = mock(DocumentoRepository.class);
    private final DocumentoProcesador procesador = new DocumentoProcesador(repo);

    @Test
    void generaComprobanteNumerado() {
        procesador.procesar(new EventoMensaje("e1", "PEDIDO_CREADO", 42L, 1L, 3, "c@duoc.cl", "p", "f", "{}"));
        ArgumentCaptor<Documento> d = ArgumentCaptor.forClass(Documento.class);
        verify(repo).save(d.capture());
        assertEquals("COMP-00000042", d.getValue().getNumero());
        assertTrue(d.getValue().getContenido().contains("Cantidad: 3"));
    }

    @Test
    void eventoIncompletoEsDefinitivo() {
        assertThrows(ErrorDefinitivoException.class,
                () -> procesador.procesar(new EventoMensaje("e1", "PEDIDO_CREADO", null, 1L, 3, "c", "p", "f", "{}")));
    }
}
