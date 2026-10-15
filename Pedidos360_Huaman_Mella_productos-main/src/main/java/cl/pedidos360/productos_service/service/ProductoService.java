package cl.pedidos360.productos_service.service;

import cl.pedidos360.productos_service.model.Producto;
import cl.pedidos360.productos_service.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public Producto buscarPorId(Long id) {

        return productoRepository.findById(id)
                .orElseThrow(
                        () -> new ProductoNoEncontradoException(id)
                );
    }

    public List<Producto> buscarPorMarca(String marca) {
        return productoRepository.findByMarca(marca);
    }

    public List<Producto> buscarPorCategoria(String categoria) {
        return productoRepository.findByCategoria(categoria);
    }

    public Producto crear(Producto producto) {

        producto.setId(null);

        return productoRepository.save(producto);
    }

    public Producto actualizar(Long id, Producto datos) {

        Producto existente = buscarPorId(id);

        existente.setModelo(datos.getModelo());
        existente.setMarca(datos.getMarca());
        existente.setTalla(datos.getTalla());
        existente.setPrecio(datos.getPrecio());
        existente.setCategoria(datos.getCategoria());
        existente.setDescripcion(datos.getDescripcion());

        return productoRepository.save(existente);
    }

    public void eliminar(Long id) {

        Producto producto = buscarPorId(id);

        productoRepository.delete(producto);
    }
}