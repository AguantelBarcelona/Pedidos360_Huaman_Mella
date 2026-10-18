package cl.pedidos360.mensajeria_admin_service.dlq;

import cl.pedidos360.mensajeria_admin_service.colas.ColaService;
import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.ConfirmacionRequeridaException;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;
import cl.pedidos360.mensajeria_admin_service.comun.SqsFalso;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.StartMessageMoveTaskRequest;
import software.amazon.awssdk.services.sqs.model.StartMessageMoveTaskResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DlqServiceTest {

    private final SqsClient sqs = SqsFalso.crear();

    private DlqService servicio(boolean permitirPurga) {
        var props = SqsFalso.props(permitirPurga);
        var nombres = new NombresRecursos(props);
        var auditoria = new AuditoriaAdmin();
        return new DlqService(sqs, new ColaService(sqs, nombres, auditoria), nombres, auditoria, props);
    }

    @Test
    void reprocesarMueveDeLaDlqASuColaOrigen() {
        when(sqs.startMessageMoveTask(any(StartMessageMoveTaskRequest.class)))
                .thenReturn(StartMessageMoveTaskResponse.builder().taskHandle("tarea-1").build());

        Map<String, String> r = servicio(true).reprocesar("notificaciones",
                new DlqService.Confirmacion("REPROCESAR pedidos360-notificaciones-dlq"));

        ArgumentCaptor<StartMessageMoveTaskRequest> req = ArgumentCaptor.forClass(StartMessageMoveTaskRequest.class);
        verify(sqs).startMessageMoveTask(req.capture());
        assertEquals("arn:aws:sqs:us-east-1:123:pedidos360-notificaciones-dlq", req.getValue().sourceArn());
        assertEquals("arn:aws:sqs:us-east-1:123:pedidos360-notificaciones", req.getValue().destinationArn());
        assertEquals(1, req.getValue().maxNumberOfMessagesPerSecond());
        assertEquals("tarea-1", r.get("tarea"));
    }

    @Test
    void reprocesarSinConfirmacionNoHaceNada() {
        assertThrows(ConfirmacionRequeridaException.class,
                () -> servicio(true).reprocesar("notificaciones", new DlqService.Confirmacion("si")));
        verify(sqs, never()).startMessageMoveTask(any(StartMessageMoveTaskRequest.class));
    }

    @Test
    void purgarExigeTextoExacto() {
        DlqService s = servicio(true);
        assertThrows(ConfirmacionRequeridaException.class, () -> s.purgar("documentos", null));
        verify(sqs, never()).purgeQueue(any(PurgeQueueRequest.class));

        s.purgar("documentos", new DlqService.Confirmacion("VACIAR pedidos360-documentos-dlq"));
        ArgumentCaptor<PurgeQueueRequest> req = ArgumentCaptor.forClass(PurgeQueueRequest.class);
        verify(sqs).purgeQueue(req.capture());
        assertEquals("https://sqs/pedidos360-documentos-dlq", req.getValue().queueUrl());
    }

    @Test
    void purgaSoloAfectaDlqNuncaLaColaOrigen() {
        DlqService s = servicio(true);
        // "documentos" siempre se traduce a su DLQ; la cola origen no se puede purgar
        assertEquals("pedidos360-documentos-dlq", s.nombreDlq("documentos"));
        assertEquals("pedidos360-documentos-dlq", s.nombreDlq("pedidos360-documentos-dlq"));
    }

    @Test
    void purgaDeshabilitadaPorConfiguracion() {
        assertThrows(IllegalArgumentException.class, () -> servicio(false).purgar("documentos",
                new DlqService.Confirmacion("VACIAR pedidos360-documentos-dlq")));
        verify(sqs, never()).purgeQueue(any(PurgeQueueRequest.class));
    }
}
