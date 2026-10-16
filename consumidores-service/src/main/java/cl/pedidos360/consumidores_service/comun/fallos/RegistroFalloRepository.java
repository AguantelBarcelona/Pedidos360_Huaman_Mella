package cl.pedidos360.consumidores_service.comun.fallos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroFalloRepository extends JpaRepository<RegistroFallo, Long> {

    List<RegistroFallo> findTop100ByOrderByFechaDesc();

    List<RegistroFallo> findTop100ByDominioOrderByFechaDesc(String dominio);
}
