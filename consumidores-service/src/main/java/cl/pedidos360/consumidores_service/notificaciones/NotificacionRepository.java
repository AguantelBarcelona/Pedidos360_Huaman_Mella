package cl.pedidos360.consumidores_service.notificaciones;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findTop50ByOrderByEnviadaEnDesc();
}
