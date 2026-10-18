package cl.pedidos360.mensajeria_admin_service.comun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Deja en el log quien ejecuto cada operacion administrativa. */
@Component
public class AuditoriaAdmin {

    private static final Logger log = LoggerFactory.getLogger("ADMIN_AUDIT");

    public void registrar(String operacion, String recurso, String detalle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth == null ? "desconocido" : auth.getName();
        log.info("usuario={} operacion={} recurso={} detalle={}", usuario, operacion, recurso, detalle);
    }
}
