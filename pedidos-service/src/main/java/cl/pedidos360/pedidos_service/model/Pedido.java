package cl.pedidos360.pedidos_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El correo del cliente es obligatorio")
    @Email(message = "El correo del cliente no es válido")
    @Column(nullable = false)
    private String clienteEmail;

    // Identificador único del usuario autenticado en Cognito.
    // Se obtiene desde el claim "sub" del JWT.
    // Se mantiene nullable para no afectar pedidos antiguos.
    @Column
    private String clienteSub;

    @NotNull(message = "El producto es obligatorio")
    @Min(value = 1, message = "El producto debe tener un ID válido")
    @Column(nullable = false)
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    @Column(nullable = false)
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @Min(value = 1, message = "El precio debe ser mayor a 0")
    @Column(nullable = false)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    public Pedido() {
    }

    public Pedido(
            Long id,
            String clienteEmail,
            Long productoId,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal total,
            String estado,
            LocalDateTime fechaCreacion) {

        this.id = id;
        this.clienteEmail = clienteEmail;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.total = total;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    @PrePersist
    public void alCrear() {

        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }

        if (estado == null || estado.isBlank()) {
            estado = "CREADO";
        }

        calcularTotal();
    }

    @PreUpdate
    public void alActualizar() {
        calcularTotal();
    }

    private void calcularTotal() {

        if (precioUnitario != null && cantidad != null) {
            total = precioUnitario.multiply(
                    BigDecimal.valueOf(cantidad)
            );
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public String getClienteSub() {
        return clienteSub;
    }

    public void setClienteSub(String clienteSub) {
        this.clienteSub = clienteSub;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}