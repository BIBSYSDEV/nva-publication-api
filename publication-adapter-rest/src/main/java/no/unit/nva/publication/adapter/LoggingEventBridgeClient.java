package no.unit.nva.publication.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/**
 * Logs the events a handler emits instead of delivering them. There is no event bus yet — the
 * event-handler adapter is a separate piece of work — and the previous stand-in dropped them
 * silently, which made the gap invisible. Logging at WARN keeps it visible until a real bus
 * (Kafka or NATS) replaces this.
 */
public final class LoggingEventBridgeClient implements EventBridgeClient {

    private static final String SERVICE_NAME = "eventbridge";
    private static final Logger logger = LoggerFactory.getLogger(LoggingEventBridgeClient.class);

    @Override
    public PutEventsResponse putEvents(PutEventsRequest request) {
        request.entries().forEach(entry -> logger.warn(
            "Event not delivered, no bus configured: source={} detailType={} bus={} detail={}",
            entry.source(), entry.detailType(), entry.eventBusName(), entry.detail()));
        return PutEventsResponse.builder().failedEntryCount(0).build();
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public void close() {
        // nothing to release
    }
}
