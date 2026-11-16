package cl.pedidos360.consumidores_kinesis_service.procesamiento;

import cl.pedidos360.consumidores_kinesis_service.eventos.PedidoKinesisEvent;
import cl.pedidos360.consumidores_kinesis_service.eventos.InventarioKinesisEvent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Service
public class ProcesadorEventos {

    private final JdbcTemplate jdbc;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private static final Set<String> TIPOS_INVENTARIO = Set.of(
            "INVENTARIO_CREADO",
            "STOCK_ACTUALIZADO",
            "STOCK_RESERVADO",
            "STOCK_LIBERADO"
    );

    public ProcesadorEventos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Resultado(String eventId, boolean nuevo) {}

    @Transactional
    public Resultado procesar(
            String aplicacion,
            DestinoConsumidor destino,
            String partitionKey,
            String payload) {

        return switch (destino) {
            case PEDIDOS_ANALITICA, PEDIDOS_TRAZABILIDAD ->
                    procesarPedido(aplicacion, destino, partitionKey, payload);

            case INVENTARIO_PROYECCION ->
                    procesarInventario(aplicacion, partitionKey, payload);
        };
    }

    private Resultado procesarPedido(
            String aplicacion,
            DestinoConsumidor destino,
            String partitionKey,
            String payload) {

        PedidoKinesisEvent evento =
                mapper.readValue(payload, PedidoKinesisEvent.class);

        validar(evento != null, "Evento de pedido nulo");

        UUID eventId = UUID.fromString(evento.eventId());

        validar("PEDIDO_CREADO".equals(evento.eventType()),
                "Tipo de evento de pedido invalido");

        validar(evento.pedidoId() != null && evento.pedidoId() > 0,
                "Pedido ID invalido");

        validar(evento.productoId() != null && evento.productoId() > 0,
                "Producto ID invalido");

        validar(evento.cantidad() != null && evento.cantidad() > 0,
                "Cantidad invalida");

        validar(evento.total() != null && evento.total().signum() >= 0,
                "Total invalido");

        validar("pedidos-service".equals(evento.origen()),
                "Origen invalido");

        validar("CREADO".equals(evento.estado()),
                "Estado del pedido invalido");

        validar(evento.pedidoId().toString().equals(partitionKey),
                "Partition key invalida");

        OffsetDateTime fecha = fecha(evento.fechaEvento());

        if (!registrar(aplicacion, eventId, evento.eventType())) {
            return new Resultado(evento.eventId(), false);
        }

        if (destino == DestinoConsumidor.PEDIDOS_ANALITICA) {

            jdbc.update("""
                INSERT INTO mensajeria.kinesis_pedidos_analitica
                (event_id, pedido_id, producto_id, cantidad, total, fecha_evento)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                eventId,
                evento.pedidoId(),
                evento.productoId(),
                evento.cantidad(),
                evento.total(),
                fecha
            );

        } else {

            jdbc.update("""
                INSERT INTO mensajeria.kinesis_pedidos_trazabilidad
                (event_id, pedido_id, event_type, estado, origen, fecha_evento)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                eventId,
                evento.pedidoId(),
                evento.eventType(),
                evento.estado(),
                evento.origen(),
                fecha
            );
        }

        return new Resultado(evento.eventId(), true);
    }

    private Resultado procesarInventario(
            String aplicacion,
            String partitionKey,
            String payload) {

        InventarioKinesisEvent evento =
                mapper.readValue(payload, InventarioKinesisEvent.class);

        validar(evento != null, "Evento de inventario nulo");

        UUID eventId = UUID.fromString(evento.eventId());

        validar(evento.eventType() != null
                        && TIPOS_INVENTARIO.contains(evento.eventType()),
                "Tipo de evento de inventario invalido");

        validar(evento.productoId() != null && evento.productoId() > 0,
                "Producto ID invalido");

        validar(evento.stockDisponible() != null
                        && evento.stockDisponible() >= 0,
                "Stock disponible invalido");

        validar(evento.stockReservado() != null
                        && evento.stockReservado() >= 0,
                "Stock reservado invalido");

        validar("inventario-service".equals(evento.origen()),
                "Origen invalido");

        validar(evento.productoId().toString().equals(partitionKey),
                "Partition key invalida");

        OffsetDateTime fecha = fecha(evento.fechaEvento());

        if (!registrar(aplicacion, eventId, evento.eventType())) {
            return new Resultado(evento.eventId(), false);
        }

        jdbc.update("""
            INSERT INTO mensajeria.kinesis_inventario_proyeccion AS actual
            (producto_id, event_id, event_type, stock_disponible,
             stock_reservado, fecha_evento)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (producto_id) DO UPDATE SET
                event_id = EXCLUDED.event_id,
                event_type = EXCLUDED.event_type,
                stock_disponible = EXCLUDED.stock_disponible,
                stock_reservado = EXCLUDED.stock_reservado,
                fecha_evento = EXCLUDED.fecha_evento
            WHERE actual.fecha_evento < EXCLUDED.fecha_evento
            """,
            evento.productoId(),
            eventId,
            evento.eventType(),
            evento.stockDisponible(),
            evento.stockReservado(),
            fecha
        );

        return new Resultado(evento.eventId(), true);
    }

    private boolean registrar(
            String aplicacion,
            UUID eventId,
            String eventType) {

        return jdbc.update("""
            INSERT INTO mensajeria.kinesis_eventos_procesados
            (aplicacion, event_id, event_type)
            VALUES (?, ?, ?)
            ON CONFLICT (aplicacion, event_id) DO NOTHING
            """,
            aplicacion,
            eventId,
            eventType
        ) == 1;
    }

    private OffsetDateTime fecha(String valor) {
        return Instant.parse(valor).atOffset(ZoneOffset.UTC);
    }

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
