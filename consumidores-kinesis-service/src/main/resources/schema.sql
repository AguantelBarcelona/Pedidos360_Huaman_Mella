CREATE TABLE IF NOT EXISTS mensajeria.kinesis_eventos_procesados (
    aplicacion VARCHAR(150) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    procesado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (aplicacion, event_id)
);

CREATE TABLE IF NOT EXISTS mensajeria.kinesis_pedidos_analitica (
    event_id UUID PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL,
    total NUMERIC NOT NULL,
    fecha_evento TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS mensajeria.kinesis_pedidos_trazabilidad (
    event_id UUID PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    origen VARCHAR(100) NOT NULL,
    fecha_evento TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS mensajeria.kinesis_inventario_proyeccion (
    producto_id BIGINT PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    stock_disponible INTEGER NOT NULL,
    stock_reservado INTEGER NOT NULL,
    fecha_evento TIMESTAMPTZ NOT NULL
);
