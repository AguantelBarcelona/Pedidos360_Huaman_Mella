CREATE SCHEMA IF NOT EXISTS mensajeria;

CREATE TABLE IF NOT EXISTS mensajeria.mensajes_procesados (
    event_id VARCHAR(100) NOT NULL,
    consumidor VARCHAR(40) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    sqs_message_id VARCHAR(100) NOT NULL,
    procesado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_mensajes_procesados
        PRIMARY KEY (event_id, consumidor)
);
