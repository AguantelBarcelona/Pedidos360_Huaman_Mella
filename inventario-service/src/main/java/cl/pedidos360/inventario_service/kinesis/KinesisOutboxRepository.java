package cl.pedidos360.inventario_service.kinesis;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface KinesisOutboxRepository
        extends JpaRepository<KinesisOutboxEvent, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM KinesisOutboxEvent e
            WHERE e.publicadoEn IS NULL
            ORDER BY e.creadoEn ASC, e.eventId ASC
            """)
    List<KinesisOutboxEvent> pendientes(Pageable limite);
}