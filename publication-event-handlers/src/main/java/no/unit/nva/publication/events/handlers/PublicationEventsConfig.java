package no.unit.nva.publication.events.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public final class PublicationEventsConfig {
    
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    public static final Environment ENVIRONMENT = new Environment();
    public static final String EVENTS_BUCKET = ENVIRONMENT.readEnv("EVENTS_BUCKET");
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");
    public static final S3Client  DEFAULT_S3_CLIENT = S3Driver.defaultS3Client().build();
    
    private PublicationEventsConfig() {
    
    }
    
    @JacocoGenerated
    public static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
            .region(Region.of(AWS_REGION))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();
    }
}
