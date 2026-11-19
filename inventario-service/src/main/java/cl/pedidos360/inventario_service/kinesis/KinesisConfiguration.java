package cl.pedidos360.inventario_service.kinesis;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;

@Configuration
@EnableScheduling
public class KinesisConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            name = "app.kinesis.enabled",
            havingValue = "true"
    )
    public KinesisClient kinesisClient(
            @Value("${app.kinesis.aws-region}") String region,
            @Value("${app.kinesis.timeout-ms}") long timeoutMs) {

        return KinesisClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(config ->
                        config.apiCallTimeout(
                                Duration.ofMillis(timeoutMs)))
                .build();
    }
}