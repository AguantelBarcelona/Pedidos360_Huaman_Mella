package cl.pedidos360.productos_service.repository;

import cl.pedidos360.productos_service.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository
        extends JpaRepository<Producto, Long> {

    List<Producto> findByMarca(String marca);

    List<Producto> findByCategoria(String categoria);
}