package cl.pedidos360.pedidos_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearPedidoRequest(

        @NotBlank(message = "El correo del cliente es obligatorio")
        @Email(message = "El correo del cliente no es válido")
        String clienteEmail,

        @NotNull(message = "El producto es obligatorio")
        @Min(value = 1, message = "El producto debe tener un ID válido")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a 0")
        Integer cantidad

) {
}