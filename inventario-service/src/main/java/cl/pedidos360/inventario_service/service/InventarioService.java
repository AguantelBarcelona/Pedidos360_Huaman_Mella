package cl.pedidos360.inventario_service.service;

import cl.pedidos360.inventario_service.model.Inventario;
import cl.pedidos360.inventario_service.repository.InventarioRepository;
import cl.pedidos360.inventario_service.kinesis.KinesisOutboxService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final KinesisOutboxService kinesisOutboxService;

    public InventarioService(
            InventarioRepository inventarioRepository,
            KinesisOutboxService kinesisOutboxService) {

        this.inventarioRepository = inventarioRepository;
        this.kinesisOutboxService = kinesisOutboxService;
    }

    public List<Inventario> listarTodos() {

        return inventarioRepository.findAll();
    }

    public Inventario buscarPorProducto(Long productoId) {

        return inventarioRepository
                .findByProductoId(productoId)
                .orElseThrow(
                        () -> new InventarioNoEncontradoException(productoId)
                );
    }

    @Transactional
    public Inventario crear(Inventario inventario) {

        inventario.setId(null);

        if (inventario.getStockReservado() == null) {
            inventario.setStockReservado(0);
        }

        Inventario guardado =
                inventarioRepository.save(inventario);

        // EV3 - Evento Kinesis
        kinesisOutboxService.registrarCambio(
                "INVENTARIO_CREADO", guardado);

        return guardado;
    }

    @Transactional
    public Inventario actualizarStock(
            Long productoId,
            Integer nuevoStock) {

        Inventario inventario =
                buscarPorProducto(productoId);

        inventario.setStockDisponible(nuevoStock);

        Inventario guardado =
                inventarioRepository.save(inventario);

        // EV3 - Evento Kinesis
        kinesisOutboxService.registrarCambio(
                "STOCK_ACTUALIZADO", guardado);

        return guardado;
    }

    @Transactional
    public Inventario reservarStock(
            Long productoId,
            Integer cantidad) {

        Inventario inventario =
                buscarPorProducto(productoId);

        if (inventario.getStockDisponible() < cantidad) {

            throw new StockInsuficienteException(productoId);
        }

        inventario.setStockDisponible(
                inventario.getStockDisponible() - cantidad
        );

        inventario.setStockReservado(
                inventario.getStockReservado() + cantidad
        );

        Inventario guardado =
                inventarioRepository.save(inventario);

        // EV3 - Evento Kinesis
        kinesisOutboxService.registrarCambio(
                "STOCK_RESERVADO", guardado);

        return guardado;
    }

    @Transactional
    public Inventario liberarStock(
            Long productoId,
            Integer cantidad) {

        Inventario inventario =
                buscarPorProducto(productoId);

        if (inventario.getStockReservado() < cantidad) {
            throw new IllegalArgumentException(
                    "No existe suficiente stock reservado"
            );
        }

        inventario.setStockReservado(
                inventario.getStockReservado() - cantidad
        );

        inventario.setStockDisponible(
                inventario.getStockDisponible() + cantidad
        );

        Inventario guardado =
                inventarioRepository.save(inventario);

        // EV3 - Evento Kinesis
        kinesisOutboxService.registrarCambio(
                "STOCK_LIBERADO", guardado);

        return guardado;
    }
}