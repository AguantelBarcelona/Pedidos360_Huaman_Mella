package cl.pedidos360.mensajeria_admin_service.suscripciones;

import cl.pedidos360.mensajeria_admin_service.colas.ColaService;
import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import cl.pedidos360.mensajeria_admin_service.comun.NombresRecursos;
import cl.pedidos360.mensajeria_admin_service.comun.SqsFalso;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SuscripcionServiceTest {

    private static final String TOPICO = "arn:aws:sns:us-east-1:123:pedidos360-eventos";
    private static final String COLA = "arn:aws:sqs:us-east-1:123:pedidos360-pagos";

    private final SqsClient sqs = SqsFalso.crear();
    private final JsonMapper json = JsonMapper.builder().build();
    private final NombresRecursos nombres = new NombresRecursos(SqsFalso.props(true));
    private final SuscripcionService service = new SuscripcionService(mock(SnsClient.class), sqs,
            new ColaService(sqs, nombres, new AuditoriaAdmin()), nombres, new AuditoriaAdmin(), json);

    @Test
    void filtroPorAtributoEventType() {
        assertEquals("{\"eventType\":[\"PEDIDO_CREADO\",\"PEDIDO_CANCELADO\"]}",
                service.filtro(List.of("PEDIDO_CREADO", "PEDIDO_CANCELADO")));
        assertNull(service.filtro(List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.filtro(List.of("pedido creado")));
    }

    @Test
    void agregaPermisoSnsSinBorrarLosExistentes() throws Exception {
        String existente = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"Otro\",\"Effect\":\"Allow\"}]}";
        when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(
                GetQueueAttributesResponse.builder().attributesWithStrings(Map.of("Policy", existente)).build());

        service.permitirTopico("https://sqs/pedidos360-pagos", COLA, TOPICO);

        ArgumentCaptor<SetQueueAttributesRequest> req = ArgumentCaptor.forClass(SetQueueAttributesRequest.class);
        verify(sqs).setQueueAttributes(req.capture());
        var politica = json.readTree(req.getValue().attributesAsStrings().get("Policy"));
        assertEquals(2, politica.get("Statement").size());
        assertEquals("Otro", politica.get("Statement").get(0).get("Sid").asString());
        assertEquals(TOPICO, politica.get("Statement").get(1).get("Condition").get("ArnEquals").get("aws:SourceArn").asString());
    }

    @Test
    void noDuplicaPermisoSiYaExiste() {
        String yaPermitido = "{\"Statement\":[{\"Resource\":\"" + COLA + "\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"" + TOPICO + "\"}}}]}";
        when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(
                GetQueueAttributesResponse.builder().attributesWithStrings(Map.of("Policy", yaPermitido)).build());

        service.permitirTopico("https://sqs/pedidos360-pagos", COLA, TOPICO);

        verify(sqs, never()).setQueueAttributes(any(SetQueueAttributesRequest.class));
    }
}
