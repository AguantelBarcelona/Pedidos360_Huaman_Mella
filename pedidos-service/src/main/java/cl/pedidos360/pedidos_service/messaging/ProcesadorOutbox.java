package cl.pedidos360.pedidos_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class ProcesadorOutbox {
    private static final Logger log = LoggerFactory.getLogger(ProcesadorOutbox.class);
    private final EventoOutboxRepository repository;
    private final PublicadorSns publicador;

    public ProcesadorOutbox(EventoOutboxRepository repository, PublicadorSns publicador) {
        this.repository = repository;
        this.publicador = publicador;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.messaging.poll-delay-ms:10000}")
    public void publicarPendientes() {
        for (EventoOutbox evento : repository.pendientesParaPublicar(PageRequest.of(0, 10))) {
            try {
                String snsMessageId = publicador.publicar(evento);
                evento.publicado();
                log.info("SNS publicado eventId={} tipo={} snsMessageId={}",
                        evento.getEventId(), evento.getEventType(), snsMessageId);
            } catch (Exception fallo) {
                evento.error(fallo);
                log.error("SNS error eventId={} intento={} causa={} estado={}",
                        evento.getEventId(), evento.getIntentos(),
                        evento.getUltimoError(), evento.getEstado());
            }
        }
    }
}
