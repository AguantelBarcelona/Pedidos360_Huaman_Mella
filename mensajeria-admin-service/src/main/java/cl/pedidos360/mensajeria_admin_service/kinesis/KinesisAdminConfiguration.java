package cl.pedidos360.mensajeria_admin_service.kinesis;

import cl.pedidos360.mensajeria_admin_service.comun.AdminProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Duration;

@Configuration
public class KinesisAdminConfiguration {

    @Bean(destroyMethod = "close")
    public KinesisClient kinesisClient(AdminProperties props) {
        return KinesisClient.builder()
                .region(Region.of(props.awsRegion()))
                .overrideConfiguration(c ->
                        c.apiCallTimeout(Duration.ofSeconds(15)))
                .build();
    }

    @Bean(destroyMethod = "close")
    public DynamoDbClient dynamoDbClient(AdminProperties props) {
        return DynamoDbClient.builder()
                .region(Region.of(props.awsRegion()))
                .overrideConfiguration(c ->
                        c.apiCallTimeout(Duration.ofSeconds(15)))
                .build();
    }
}
