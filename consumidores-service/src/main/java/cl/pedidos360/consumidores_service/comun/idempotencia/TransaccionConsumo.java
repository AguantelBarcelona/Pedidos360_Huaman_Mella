package cl.pedidos360.consumidores_service.comun.idempotencia;

import cl.pedidos360.consumidores_service.comun.evento.EventoMensaje;
import cl.pedidos360.consumidores_service.comun.evento.ProcesadorEvento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransaccionConsumo {

    private final EventoProcesadoRepository procesados;

    public TransaccionConsumo(EventoProcesadoRepository procesados) {
        this.procesados = procesados;
    }

    public boolean yaProcesado(String dominio, String eventId) {
        return procesados.existsById(EventoProcesado.clave(dominio, eventId));
    }

    /** @return false si el evento ya estaba procesado (duplicado), true si se proceso ahora. */
    @Transactional
    public boolean procesarUnaVez(ProcesadorEvento procesador, EventoMensaje evento,
                                  String messageId, String instancia, int intento) {
        if (yaProcesado(procesador.dominio(), evento.eventId())) {
            return false;
        }
        procesador.procesar(evento);
        procesados.saveAndFlush(new EventoProcesado(
                procesador.dominio(), evento.eventId(), evento.eventType(),
                messageId, instancia, intento));
        return true;
    }
}
