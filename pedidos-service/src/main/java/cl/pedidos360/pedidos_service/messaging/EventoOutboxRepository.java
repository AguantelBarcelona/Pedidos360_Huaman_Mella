package cl.pedidos360.pedidos_service.messaging;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventoOutboxRepository extends JpaRepository<EventoOutbox, String> {

    // Bloquea los eventos pendientes durante el envio para evitar publicaciones
    // simultaneas del mismo registro entre dos instancias del publicador.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventoOutbox e where e.estado = 'PENDIENTE' order by e.creadoEn asc")
    List<EventoOutbox> pendientesParaPublicar(Pageable limite);
}
