package no.unit.nva.publication.events.dynamodbstream;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;

/**
 * Publishing event.
 */
public interface EventPublisher {

    /**
     * Publish DynamodbEvent on a bus.
     *
     * @param event DynamoDB stream event.
     */
    void publish(DynamodbEvent event);
}
