package cl.pedidos360.pedidos_service.client;

import cl.pedidos360.pedidos_service.service.InventarioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventarioClient {

    private final RestClient restClient;

    public InventarioClient(
            RestClient.Builder restClientBuilder,
            @Value("${inventario.service.url}") String inventarioServiceUrl) {

        this.restClient = restClientBuilder
                .baseUrl(inventarioServiceUrl)
                .build();
    }

    public InventarioResponse obtenerInventario(Long productoId) {

        return restClient
                .get()
                .uri("/api/inventario/producto/{id}", productoId)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new InventarioException(
                                    "No se pudo obtener el inventario del producto "
                                            + productoId
                            );
                        }
                )
                .body(InventarioResponse.class);
    }

    public InventarioResponse reservarStock(
            Long productoId,
            Integer cantidad) {

        return restClient
                .post()
                .uri(
                        uriBuilder -> uriBuilder
                                .path("/api/inventario/producto/{id}/reservar")
                                .queryParam("cantidad", cantidad)
                                .build(productoId)
                )
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new InventarioException(
                                    "No se pudo reservar stock para el producto "
                                            + productoId
                            );
                        }
                )
                .body(InventarioResponse.class);
    }
}