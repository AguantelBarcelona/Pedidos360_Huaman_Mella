package cl.pedidos360.consumidores_service.documentos;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoRepository repository;

    public DocumentoController(DocumentoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Documento> ultimos() {
        return repository.findTop50ByOrderByGeneradoEnDesc();
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<Documento> porPedido(@PathVariable Long pedidoId) {
        return repository.findByPedidoId(pedidoId);
    }
}
