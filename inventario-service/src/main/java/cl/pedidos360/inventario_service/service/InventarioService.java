package cl.pedidos360.inventario_service.service;

import cl.pedidos360.inventario_service.model.Inventario;
import cl.pedidos360.inventario_service.repository.InventarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;

    public InventarioService(
            InventarioRepository inventarioRepository) {

        this.inventarioRepository = inventarioRepository;
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

    public Inventario crear(Inventario inventario) {

        inventario.setId(null);

        if (inventario.getStockReservado() == null) {
            inventario.setStockReservado(0);
        }

        return inventarioRepository.save(inventario);
    }

    public Inventario actualizarStock(
            Long productoId,
            Integer nuevoStock) {

        Inventario inventario =
                buscarPorProducto(productoId);

        inventario.setStockDisponible(nuevoStock);

        return inventarioRepository.save(inventario);
    }

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

        return inventarioRepository.save(inventario);
    }

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

        return inventarioRepository.save(inventario);
    }
}