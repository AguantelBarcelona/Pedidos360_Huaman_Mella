package cl.pedidos360.pedidos_service.repository;

import cl.pedidos360.pedidos_service.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteEmail(String clienteEmail);

    List<Pedido> findByClienteSub(String clienteSub);
}
