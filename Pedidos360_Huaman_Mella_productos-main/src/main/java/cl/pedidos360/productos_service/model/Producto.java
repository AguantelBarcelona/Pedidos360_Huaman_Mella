package cl.pedidos360.productos_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El modelo es obligatorio")
    @Column(nullable = false)
    private String modelo;

    @NotBlank(message = "La marca es obligatoria")
    @Column(nullable = false)
    private String marca;

    @NotNull(message = "La talla es obligatoria")
    @Min(value = 30, message = "La talla debe ser válida")
    @Column(nullable = false)
    private Integer talla;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "1.0", message = "El precio debe ser mayor que 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotBlank(message = "La categoría es obligatoria")
    @Column(nullable = false)
    private String categoria;

    @Column(length = 500)
    private String descripcion;

    public Producto() {
    }

    public Producto(
            Long id,
            String modelo,
            String marca,
            Integer talla,
            BigDecimal precio,
            String categoria,
            String descripcion) {

        this.id = id;
        this.modelo = modelo;
        this.marca = marca;
        this.talla = talla;
        this.precio = precio;
        this.categoria = categoria;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public Integer getTalla() {
        return talla;
    }

    public void setTalla(Integer talla) {
        this.talla = talla;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}