package cl.pedidos360.consumidores_service.comun.sqs;

import cl.pedidos360.consumidores_service.auditoria.AuditoriaProcesador;
import cl.pedidos360.consumidores_service.auditoria.RegistroAuditoriaRepository;
import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;
import cl.pedidos360.consumidores_service.documentos.DocumentoProcesador;
import cl.pedidos360.consumidores_service.documentos.DocumentoRepository;
import cl.pedidos360.consumidores_service.notificaciones.NotificacionProcesador;
import cl.pedidos360.consumidores_service.notificaciones.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EscuchaSqsTest {

    @Test
    void cadaProcesadorDeclaraSuDominioConLaAnotacion() {
        assertEquals("notificaciones", new NotificacionProcesador(mock(NotificacionRepository.class)).dominio());
        assertEquals("documentos", new DocumentoProcesador(mock(DocumentoRepository.class)).dominio());
        assertEquals("auditoria", new AuditoriaProcesador(mock(RegistroAuditoriaRepository.class)).dominio());
    }

    @Test
    void laAnotacionRegistraLaClaseComoBeanDeSpring() {
        assertNotNull(AnnotationUtils.findAnnotation(NotificacionProcesador.class, Component.class));
    }

    @Test
    void procesadorSinAnotacionFallaConMensajeClaro() {
        ProcesadorEvento sinAnotacion = new ProcesadorEvento() {
            @Override
            public void procesar(EventoMensaje evento) { }
        };
        IllegalStateException e = assertThrows(IllegalStateException.class, sinAnotacion::dominio);
        assertTrue(e.getMessage().contains("@EscuchaSqs"));
    }
}
