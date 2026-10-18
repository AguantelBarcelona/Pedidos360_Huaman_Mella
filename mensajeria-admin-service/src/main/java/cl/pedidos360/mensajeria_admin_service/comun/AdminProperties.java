package cl.pedidos360.mensajeria_admin_service.comun;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String awsRegion, String prefijo, boolean permitirPurga, List<String> consumidoresUrls) {

    public AdminProperties {
        if (awsRegion == null || awsRegion.isBlank()) awsRegion = "us-east-1";
        if (prefijo == null || prefijo.isBlank()) prefijo = "pedidos360";
        if (consumidoresUrls == null) consumidoresUrls = List.of();
    }
}
