package cl.pedidos360.consumidores_service.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    List<RegistroAuditoria> findTop100ByOrderByRegistradoEnDesc();

    List<RegistroAuditoria> findTop100ByEventTypeOrderByRegistradoEnDesc(String eventType);
}
