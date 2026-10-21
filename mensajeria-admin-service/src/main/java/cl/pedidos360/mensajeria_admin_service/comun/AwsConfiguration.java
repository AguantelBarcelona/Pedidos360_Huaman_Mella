package cl.pedidos360.mensajeria_admin_service.comun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfiguration {

    @Bean(destroyMethod = "close")
    public SqsClient sqsClient(AdminProperties props) {
        return SqsClient.builder().region(Region.of(props.awsRegion())).build();
    }

    @Bean(destroyMethod = "close")
    public SnsClient snsClient(AdminProperties props) {
        return SnsClient.builder().region(Region.of(props.awsRegion())).build();
    }

    @Bean(destroyMethod = "close")
    public CloudWatchClient cloudWatchClient(AdminProperties props) {
        return CloudWatchClient.builder().region(Region.of(props.awsRegion())).build();
    }
}
