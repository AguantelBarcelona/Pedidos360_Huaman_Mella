package cl.pedidos360.mensajeria_admin_service.consumidores;

import cl.pedidos360.mensajeria_admin_service.comun.AdminProperties;
import cl.pedidos360.mensajeria_admin_service.comun.AuditoriaAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pausa/reanuda los consumidores llamando a cada instancia de consumidores-service.
 * Se reenvia el mismo token ADMIN del usuario: el consumidor vuelve a validarlo.
 */
@Service
public class ConsumidoresRemotosService {

    private static final Set<String> ACCIONES = Set.of("pausar", "reanudar");

    private final RestClient http;
    private final AdminProperties props;
    private final AuditoriaAdmin auditoria;

    @Autowired
    public ConsumidoresRemotosService(AdminProperties props, AuditoriaAdmin auditoria) {
        this(RestClient.create(), props, auditoria);
    }

    ConsumidoresRemotosService(RestClient http, AdminProperties props, AuditoriaAdmin auditoria) {
        this.http = http;
        this.props = props;
        this.auditoria = auditoria;
    }

    public record RespuestaInstancia(String instancia, boolean ok, Object respuesta, String error) {
    }

    public List<RespuestaInstancia> estado(String authorization) {
        List<RespuestaInstancia> resultado = new ArrayList<>();
        for (String url : props.consumidoresUrls()) {
            resultado.add(llamar(url, "GET", "/api/consumidores", authorization));
        }
        return resultado;
    }

    /** @param instancia URL de una instancia concreta, o null para todas. */
    public List<RespuestaInstancia> accion(String dominio, String accion, String instancia, String authorization) {
        if (!ACCIONES.contains(accion)) {
            throw new IllegalArgumentException("Accion invalida: " + accion);
        }
        if (dominio == null || !dominio.matches("^[a-z]{1,40}$")) {
            throw new IllegalArgumentException("Dominio invalido");
        }
        List<String> destinos = instancia == null || instancia.isBlank()
                ? props.consumidoresUrls()
                : props.consumidoresUrls().stream().filter(u -> u.equals(instancia)).toList();
        if (destinos.isEmpty()) {
            throw new IllegalArgumentException("Instancia no configurada en CONSUMIDORES_URLS: " + instancia);
        }
        List<RespuestaInstancia> resultado = new ArrayList<>();
        for (String url : destinos) {
            resultado.add(llamar(url, "POST", "/api/consumidores/" + dominio + "/" + accion, authorization));
        }
        auditoria.registrar(accion.toUpperCase() + "_CONSUMIDOR", dominio, "instancias=" + destinos);
        return resultado;
    }

    private RespuestaInstancia llamar(String base, String metodo, String ruta, String authorization) {
        try {
            RestClient.RequestHeadersSpec<?> spec = "POST".equals(metodo)
                    ? http.post().uri(base + ruta)
                    : http.get().uri(base + ruta);
            Object cuerpo = spec.header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(Object.class);
            return new RespuestaInstancia(base, true, cuerpo, null);
        } catch (RuntimeException e) {
            // Una instancia caida no impide operar sobre las demas
            return new RespuestaInstancia(base, false, null, e.getMessage());
        }
    }
}
