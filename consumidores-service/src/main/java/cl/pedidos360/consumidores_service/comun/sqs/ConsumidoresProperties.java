package cl.pedidos360.consumidores_service.comun.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.consumidores")
public record ConsumidoresProperties(
        boolean enabled,
        String awsRegion,
        String instancia,
        List<String> activos,
        int maxMensajes,
        int esperaSegundos,
        int backoffBaseSegundos,
        int maxIntentos,
        Map<String, Cola> colas) {

    public ConsumidoresProperties {
        if (awsRegion == null || awsRegion.isBlank()) awsRegion = "us-east-1";
        if (instancia == null || instancia.isBlank()) instancia = "consumidores-local";
        if (activos == null) activos = List.of();
        if (maxMensajes < 1 || maxMensajes > 10) maxMensajes = 5;
        if (esperaSegundos < 0 || esperaSegundos > 20) esperaSegundos = 20;
        if (backoffBaseSegundos < 0) backoffBaseSegundos = 10;
        if (maxIntentos < 1) maxIntentos = 3;
        if (colas == null) colas = Map.of();
    }

    public record Cola(String url, String dlqUrl) {
    }
}
