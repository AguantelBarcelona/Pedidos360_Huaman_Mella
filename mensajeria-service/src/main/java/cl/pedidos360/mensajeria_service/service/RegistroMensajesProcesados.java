package cl.pedidos360.mensajeria_service.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegistroMensajesProcesados {

    private final JdbcTemplate jdbcTemplate;

    public RegistroMensajesProcesados(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean registrarSiNuevo(
            String eventId,
            String consumidor,
            String eventType,
            String sqsMessageId) {

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "El evento debe tener un eventId"
            );
        }

        String sql = """
                INSERT INTO mensajeria.mensajes_procesados
                (event_id, consumidor, event_type, sqs_message_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (event_id, consumidor) DO NOTHING
                """;

        int filasInsertadas = jdbcTemplate.update(
                sql,
                eventId,
                consumidor,
                eventType,
                sqsMessageId
        );

        return filasInsertadas == 1;
    }
}
