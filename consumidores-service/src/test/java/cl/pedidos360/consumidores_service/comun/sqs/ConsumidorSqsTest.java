package cl.pedidos360.consumidores_service.comun.sqs;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.errores.ErrorRecuperableException;
import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;
import cl.pedidos360.consumidores_service.comun.fallos.RegistroFallo;
import cl.pedidos360.consumidores_service.comun.fallos.RegistroFalloRepository;
import cl.pedidos360.consumidores_service.comun.fallos.SimuladorFallos;
import cl.pedidos360.consumidores_service.comun.idempotencia.TransaccionConsumo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsumidorSqsTest {

    private static final String COLA = "https://sqs.us-east-1.amazonaws.com/123/pedidos360-notificaciones";
    private static final String DLQ = "https://sqs.us-east-1.amazonaws.com/123/pedidos360-notificaciones-dlq";
    private static final String EVENTO = """
            {"eventId":"e-1","eventType":"PEDIDO_CREADO","pedidoId":10,"productoId":1,
             "cantidad":2,"clienteEmail":"cliente@duoc.cl","origen":"pedidos-service",
             "fechaEvento":"2026-10-16T04:00:00Z"}""";

    @Mock SqsClient sqs;
    @Mock TransaccionConsumo transaccion;
    @Mock RegistroFalloRepository fallos;
    @Mock ProcesadorEvento procesador;

    private SimuladorFallos simulador;
    private ConsumidorSqs consumidor;

    @BeforeEach
    void setUp() {
        simulador = new SimuladorFallos();
        when(procesador.dominio()).thenReturn("notificaciones");
        ConsumidoresProperties props = new ConsumidoresProperties(true, "us-east-1", "test-1",
                List.of("notificaciones"), 5, 0, 10, 3, Map.of());
        consumidor = new ConsumidorSqs("notificaciones", COLA, DLQ, procesador, sqs,
                JsonMapper.builder().build(), transaccion, fallos, simulador, props);
    }

    private static Message mensaje(String cuerpo, int intento) {
        return Message.builder()
                .messageId("m-1")
                .receiptHandle("rh-1")
                .body(cuerpo)
                .attributes(Map.of(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, String.valueOf(intento)))
                .messageAttributes(Map.of("eventType",
                        MessageAttributeValue.builder().dataType("String").stringValue("PEDIDO_CREADO").build()))
                .build();
    }

    @Test
    void exitoHaceAckSoloDespuesDeProcesar() {
        when(transaccion.procesarUnaVez(any(), any(), any(), any(), anyInt())).thenReturn(true);

        consumidor.procesarMensaje(mensaje(EVENTO, 1));

        var orden = inOrder(transaccion, sqs);
        orden.verify(transaccion).procesarUnaVez(eq(procesador), any(), eq("m-1"), eq("test-1"), eq(1));
        orden.verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
        assertEquals(1, consumidor.estado().procesados());
        verifyNoInteractions(fallos);
    }

    @Test
    void duplicadoSeConfirmaSinRepetirEfecto() {
        when(transaccion.procesarUnaVez(any(), any(), any(), any(), anyInt())).thenReturn(false);

        consumidor.procesarMensaje(mensaje(EVENTO, 2));

        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
        assertEquals(1, consumidor.estado().duplicados());
        assertEquals(0, consumidor.estado().procesados());
    }

    @Test
    void carreraEntreInstanciasSeTrataComoDuplicado() {
        when(transaccion.procesarUnaVez(any(), any(), any(), any(), anyInt()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(transaccion.yaProcesado("notificaciones", "e-1")).thenReturn(true);

        consumidor.procesarMensaje(mensaje(EVENTO, 1));

        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
        assertEquals(1, consumidor.estado().duplicados());
    }

    @Test
    void errorRecuperableNoHaceAckYProgramaReintento() {
        when(transaccion.procesarUnaVez(any(), any(), any(), any(), anyInt()))
                .thenThrow(new ErrorRecuperableException("BD no disponible"));

        consumidor.procesarMensaje(mensaje(EVENTO, 2));

        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
        verify(sqs, never()).sendMessage(any(SendMessageRequest.class));
        ArgumentCaptor<ChangeMessageVisibilityRequest> vis = ArgumentCaptor.forClass(ChangeMessageVisibilityRequest.class);
        verify(sqs).changeMessageVisibility(vis.capture());
        assertEquals(20, vis.getValue().visibilityTimeout()); // backoff 10 s * intento 2

        ArgumentCaptor<RegistroFallo> fallo = ArgumentCaptor.forClass(RegistroFallo.class);
        verify(fallos).save(fallo.capture());
        assertEquals("m-1", fallo.getValue().getMessageId());
        assertEquals("e-1", fallo.getValue().getEventId());
        assertEquals(2, fallo.getValue().getIntento());
        assertEquals("RECUPERABLE", fallo.getValue().getTipo());
        assertTrue(fallo.getValue().getCausa().contains("BD no disponible"));
    }

    @Test
    void ultimoIntentoRecuperableQuedaParaRedriveDeSqs() {
        simulador.configurar("notificaciones", SimuladorFallos.Modo.RECUPERABLE);

        consumidor.procesarMensaje(mensaje(EVENTO, 3));

        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
        ArgumentCaptor<RegistroFallo> fallo = ArgumentCaptor.forClass(RegistroFallo.class);
        verify(fallos).save(fallo.capture());
        assertTrue(fallo.getValue().getAccion().contains("DLQ"));
        verifyNoInteractions(transaccion);
    }

    @Test
    void errorDefinitivoVaADlqYLuegoAck() {
        when(transaccion.procesarUnaVez(any(), any(), any(), any(), anyInt()))
                .thenThrow(new ErrorDefinitivoException("sin clienteEmail"));

        consumidor.procesarMensaje(mensaje(EVENTO, 1));

        var orden = inOrder(sqs);
        ArgumentCaptor<SendMessageRequest> envio = ArgumentCaptor.forClass(SendMessageRequest.class);
        orden.verify(sqs).sendMessage(envio.capture());
        orden.verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
        assertEquals(DLQ, envio.getValue().queueUrl());
        assertEquals(EVENTO, envio.getValue().messageBody());
        assertEquals("sin clienteEmail", envio.getValue().messageAttributes().get("motivoFallo").stringValue());
        assertEquals("PEDIDO_CREADO", envio.getValue().messageAttributes().get("eventType").stringValue());
        assertEquals(1, consumidor.estado().enviadosDlq());
    }

    @Test
    void jsonInvalidoEsDefinitivo() {
        consumidor.procesarMensaje(mensaje("{esto no es json", 1));

        verify(sqs).sendMessage(any(SendMessageRequest.class));
        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
        verifyNoInteractions(transaccion);
    }

    @Test
    void siFallaEnvioADlqNoSeHaceAck() {
        when(transaccion.procesarUnaVez(any(), any(), any(), any(), anyInt()))
                .thenThrow(new ErrorDefinitivoException("dato invalido"));
        when(sqs.sendMessage(any(SendMessageRequest.class))).thenThrow(SqsException.builder().message("sin permisos").build());

        consumidor.procesarMensaje(mensaje(EVENTO, 1));

        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
        verify(sqs).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
    }

    @Test
    void errorInesperadoSeTrataComoRecuperable() {
        when(transaccion.procesarUnaVez(any(), any(), any(), any(), anyInt()))
                .thenThrow(new IllegalStateException("conexion cerrada"));

        consumidor.procesarMensaje(mensaje(EVENTO, 1));

        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
        assertEquals(1, consumidor.estado().fallosRecuperables());
    }

    @Test
    void recibirPideIntentoYAtributosAComoLaCola() {
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(mensaje(EVENTO, 1)).build());
        when(transaccion.procesarUnaVez(any(), any(), any(), any(), anyInt())).thenReturn(true);

        assertEquals(1, consumidor.recibirYProcesar());

        ArgumentCaptor<ReceiveMessageRequest> req = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqs).receiveMessage(req.capture());
        assertEquals(COLA, req.getValue().queueUrl());
        assertTrue(req.getValue().messageSystemAttributeNames()
                .contains(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT));
        assertTrue(req.getValue().messageAttributeNames().contains("All"));
    }

    @Test
    void pausarYReanudarCambianEstado() {
        consumidor.pausar();
        assertTrue(consumidor.estado().pausado());
        consumidor.reanudar();
        assertFalse(consumidor.estado().pausado());
    }
}
