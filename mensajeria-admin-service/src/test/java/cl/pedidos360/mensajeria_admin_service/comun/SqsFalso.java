package cl.pedidos360.mensajeria_admin_service.comun;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** SqsClient simulado: URL = https://sqs/<nombre>, ARN = arn:aws:sqs:us-east-1:123:<nombre>. */
public final class SqsFalso {

    private SqsFalso() { }

    public static SqsClient crear() {
        SqsClient sqs = mock(SqsClient.class);
        when(sqs.getQueueUrl(any(GetQueueUrlRequest.class))).thenAnswer(inv -> {
            GetQueueUrlRequest r = inv.getArgument(0);
            return GetQueueUrlResponse.builder().queueUrl("https://sqs/" + r.queueName()).build();
        });
        when(sqs.createQueue(any(CreateQueueRequest.class))).thenAnswer(inv -> {
            CreateQueueRequest r = inv.getArgument(0);
            return CreateQueueResponse.builder().queueUrl("https://sqs/" + r.queueName()).build();
        });
        when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenAnswer(inv -> {
            GetQueueAttributesRequest r = inv.getArgument(0);
            String nombre = r.queueUrl().substring(r.queueUrl().lastIndexOf('/') + 1);
            return GetQueueAttributesResponse.builder()
                    .attributesWithStrings(Map.of("QueueArn", "arn:aws:sqs:us-east-1:123:" + nombre))
                    .build();
        });
        return sqs;
    }

    public static AdminProperties props(boolean permitirPurga) {
        return new AdminProperties("us-east-1", "pedidos360", permitirPurga, java.util.List.of());
    }
}
