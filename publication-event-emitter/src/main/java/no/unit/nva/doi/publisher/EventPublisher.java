package no.unit.nva.doi.publisher;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.util.List;

/**
 * Publishing event.
 */
public interface EventPublisher {

    /**
     * Publish DynamoDB stream event.
     *
     * @param events DynamoDB stream event.
     */
    void publish(List<String> events);
}
