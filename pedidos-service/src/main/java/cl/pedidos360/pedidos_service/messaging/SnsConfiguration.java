package cl.pedidos360.pedidos_service.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
@EnableScheduling
public class SnsConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
    public SnsClient snsClient(@Value("${app.messaging.aws-region:us-east-1}") String region) {
        // Credenciales desde EC2 Instance Profile o cadena estandar del SDK.
        // Nunca almacenar claves en el proyecto ni en application.properties.
        return SnsClient.builder().region(Region.of(region)).build();
    }
}
