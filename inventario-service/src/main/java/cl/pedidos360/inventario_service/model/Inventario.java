package cl.pedidos360.inventario_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "inventario")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El producto es obligatorio")
    @Min(value = 1, message = "El producto debe tener un ID válido")
    @Column(nullable = false, unique = true)
    private Long productoId;

    @NotNull(message = "El stock disponible es obligatorio")
    @Min(value = 0, message = "El stock disponible no puede ser negativo")
    @Column(nullable = false)
    private Integer stockDisponible;

    @NotNull(message = "El stock reservado es obligatorio")
    @Min(value = 0, message = "El stock reservado no puede ser negativo")
    @Column(nullable = false)
    private Integer stockReservado;

    public Inventario() {
    }

    public Inventario(
            Long id,
            Long productoId,
            Integer stockDisponible,
            Integer stockReservado) {

        this.id = id;
        this.productoId = productoId;
        this.stockDisponible = stockDisponible;
        this.stockReservado = stockReservado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(Integer stockDisponible) {
        this.stockDisponible = stockDisponible;
    }

    public Integer getStockReservado() {
        return stockReservado;
    }

    public void setStockReservado(Integer stockReservado) {
        this.stockReservado = stockReservado;
    }
}