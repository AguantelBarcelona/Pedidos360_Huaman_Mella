package cl.pedidos360.mensajeria_service.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ControlConsumidores {

    private static final List<String> NOMBRES = List.of(
            "notificaciones",
            "documentos",
            "auditoria"
    );

    private final Map<String, AtomicBoolean> activos =
            new ConcurrentHashMap<>();

    public ControlConsumidores() {
        for (String nombre : NOMBRES) {
            activos.put(nombre, new AtomicBoolean(true));
        }
    }

    public boolean estaActivo(String nombre) {
        return obtenerControl(nombre).get();
    }

    public void pausar(String nombre) {
        obtenerControl(nombre).set(false);
    }

    public void reanudar(String nombre) {
        obtenerControl(nombre).set(true);
    }

    public Map<String, Boolean> obtenerEstados() {
        Map<String, Boolean> resultado = new LinkedHashMap<>();

        for (String nombre : NOMBRES) {
            resultado.put(nombre, estaActivo(nombre));
        }

        return resultado;
    }

    private AtomicBoolean obtenerControl(String nombre) {
        AtomicBoolean control = activos.get(nombre);

        if (control == null) {
            throw new IllegalArgumentException(
                    "Consumidor no reconocido: " + nombre
            );
        }

        return control;
    }
}
