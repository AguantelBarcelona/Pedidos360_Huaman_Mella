package cl.pedidos360.consumidores_service.comun.sqs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class SqsConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.consumidores.enabled", havingValue = "true")
    public SqsClient sqsClient(ConsumidoresProperties props) {
        return SqsClient.builder().region(Region.of(props.awsRegion())).build();
    }
}
