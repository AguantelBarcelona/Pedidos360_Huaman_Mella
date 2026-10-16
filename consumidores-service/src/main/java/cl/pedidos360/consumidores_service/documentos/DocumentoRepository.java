package cl.pedidos360.consumidores_service.documentos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findTop50ByOrderByGeneradoEnDesc();

    List<Documento> findByPedidoId(Long pedidoId);
}
