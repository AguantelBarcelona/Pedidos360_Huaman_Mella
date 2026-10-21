package cl.pedidos360.consumidores_service.comun.fallos;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;
import cl.pedidos360.consumidores_service.comun.errores.ErrorRecuperableException;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimuladorFallos {

    public enum Modo { NINGUNO, RECUPERABLE, DEFINITIVO }

    private final Map<String, Modo> modos = new ConcurrentHashMap<>();

    public void configurar(String dominio, Modo modo) {
        if (modo == null || modo == Modo.NINGUNO) {
            modos.remove(dominio);
        } else {
            modos.put(dominio, modo);
        }
    }

    public Modo modo(String dominio) {
        return modos.getOrDefault(dominio, Modo.NINGUNO);
    }

    public void aplicar(String dominio) {
        switch (modo(dominio)) {
            case RECUPERABLE -> throw new ErrorRecuperableException(
                    "[SIMULADO] Servicio externo de " + dominio + " no disponible (timeout)");
            case DEFINITIVO -> throw new ErrorDefinitivoException(
                    "[SIMULADO] Datos del evento rechazados por reglas de " + dominio);
            default -> { }
        }
    }
}
