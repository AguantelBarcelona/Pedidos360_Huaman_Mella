package cl.pedidos360.consumidores_service.comun.idempotencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoProcesadoRepository extends JpaRepository<EventoProcesado, String> {

    List<EventoProcesado> findTop50ByDominioOrderByProcesadoEnDesc(String dominio);

    List<EventoProcesado> findTop50ByOrderByProcesadoEnDesc();
}
