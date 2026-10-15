package cl.pedidos360.pedidos_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductoClient {

    private final RestClient restClient;

    public ProductoClient(
            RestClient.Builder restClientBuilder,
            @Value("${productos.service.url}") String productosServiceUrl) {

        this.restClient = restClientBuilder
                .baseUrl(productosServiceUrl)
                .build();
    }

    public ProductoResponse obtenerProducto(Long productoId) {

        return restClient
                .get()
                .uri("/api/productos/{id}", productoId)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        (request, response) -> {
                            throw new IllegalArgumentException(
                                    "Producto no encontrado: " + productoId
                            );
                        }
                )
                .body(ProductoResponse.class);
    }
}