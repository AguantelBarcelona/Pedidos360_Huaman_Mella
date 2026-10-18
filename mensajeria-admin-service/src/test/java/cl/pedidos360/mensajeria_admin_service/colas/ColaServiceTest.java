package cl.pedidos360.mensajeria_admin_service.colas;

import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.ConfirmacionRequeridaException;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;
import cl.pedidos360.mensajeria_admin_service.comun.SqsFalso;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ColaServiceTest {

    private final SqsClient sqs = SqsFalso.crear();
    private final ColaService service = new ColaService(sqs,
            new NombresRecursos(SqsFalso.props(true)), new AuditoriaAdmin());

    @Test
    void crearColaCreaDlqYRedrivePolicyConPoliticasDelProyecto() {
        service.crear(new ColaService.CrearColaRequest("pagos", null, null, null, null));

        ArgumentCaptor<CreateQueueRequest> req = ArgumentCaptor.forClass(CreateQueueRequest.class);
        verify(sqs, times(2)).createQueue(req.capture());
        List<CreateQueueRequest> creadas = req.getAllValues();

        CreateQueueRequest dlq = creadas.get(0);
        assertEquals("pedidos360-pagos-dlq", dlq.queueName());
        assertEquals("1209600", dlq.attributesAsStrings().get("MessageRetentionPeriod"));

        CreateQueueRequest origen = creadas.get(1);
        assertEquals("pedidos360-pagos", origen.queueName());
        assertEquals("345600", origen.attributesAsStrings().get("MessageRetentionPeriod"));
        assertEquals("120", origen.attributesAsStrings().get("VisibilityTimeout"));
        assertEquals("20", origen.attributesAsStrings().get("ReceiveMessageWaitTimeSeconds"));
        String redrive = origen.attributesAsStrings().get("RedrivePolicy");
        assertTrue(redrive.contains("arn:aws:sqs:us-east-1:123:pedidos360-pagos-dlq"));
        assertTrue(redrive.contains("\"maxReceiveCount\":\"3\""));
    }

    @Test
    void crearSinDlq() {
        service.crear(new ColaService.CrearColaRequest("pagos", false, null, null, null));
        ArgumentCaptor<CreateQueueRequest> req = ArgumentCaptor.forClass(CreateQueueRequest.class);
        verify(sqs, times(1)).createQueue(req.capture());
        assertNull(req.getValue().attributesAsStrings().get("RedrivePolicy"));
    }

    @Test
    void noPermiteCrearDirectamenteUnaDlq() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(new ColaService.CrearColaRequest("pagos-dlq", null, null, null, null)));
    }

    @Test
    void eliminarExigeConfirmacion() {
        assertThrows(ConfirmacionRequeridaException.class, () -> service.eliminar("pagos", null, true));
        verify(sqs, never()).deleteQueue(any(DeleteQueueRequest.class));

        service.eliminar("pagos", "pedidos360-pagos", true);
        verify(sqs, times(2)).deleteQueue(any(DeleteQueueRequest.class)); // cola + su DLQ
    }

    @Test
    void leeCamposDeRedrivePolicy() {
        String json = "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:123:x-dlq\",\"maxReceiveCount\":3}";
        assertEquals("arn:aws:sqs:us-east-1:123:x-dlq", ColaService.extraer(json, "deadLetterTargetArn"));
        assertEquals("3", ColaService.extraer(json, "maxReceiveCount"));
        assertNull(ColaService.extraer(json, "noExiste"));
    }
}
