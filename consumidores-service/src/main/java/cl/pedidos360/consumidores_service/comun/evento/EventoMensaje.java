package cl.pedidos360.consumidores_service.comun.evento;

import cl.pedidos360.consumidores_service.comun.errores.ErrorDefinitivoException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;


public record EventoMensaje(
        String eventId,
        String eventType,
        Long pedidoId,
        Long productoId,
        Integer cantidad,
        String clienteEmail,
        String origen,
        String fechaEvento,
        String payload) {

    
    public static EventoMensaje desdeJson(String cuerpo, String eventTypeAtributo, JsonMapper json) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ErrorDefinitivoException("Mensaje vacio");
        }
        JsonNode raiz;
        try {
            raiz = json.readTree(cuerpo);
        } catch (JacksonException e) {
            throw new ErrorDefinitivoException("JSON invalido: " + e.getOriginalMessage(), e);
        }
        if (raiz == null || !raiz.isObject()) {
            throw new ErrorDefinitivoException("El mensaje no es un objeto JSON");
        }

    
        if ("Notification".equals(texto(raiz, "Type")) && raiz.hasNonNull("Message")) {
            return desdeJson(raiz.get("Message").asString(), eventTypeAtributo, json);
        }

        String eventId = texto(raiz, "eventId");
        if (eventId == null || eventId.isBlank()) {
            throw new ErrorDefinitivoException("El evento no trae eventId: no se puede garantizar idempotencia");
        }
        String eventType = texto(raiz, "eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = eventTypeAtributo;
        }
        if (eventType == null || eventType.isBlank()) {
            throw new ErrorDefinitivoException("El evento no trae eventType");
        }

        return new EventoMensaje(
                eventId,
                eventType,
                numero(raiz, "pedidoId"),
                numero(raiz, "productoId"),
                raiz.hasNonNull("cantidad") ? raiz.get("cantidad").asInt() : null,
                texto(raiz, "clienteEmail"),
                texto(raiz, "origen"),
                texto(raiz, "fechaEvento"),
                cuerpo);
    }

    private static String texto(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) ? nodo.get(campo).asString() : null;
    }

    private static Long numero(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) ? nodo.get(campo).asLong() : null;
    }
}
