package no.unit.nva.cristin.lambda;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public final class ApplicationConstants {

    public static final String EMPTY_STRING = "";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String EVENT_BUS_ENV_VARIABLE = "EVENT_BUS";
    public static final String AWS_REGION_ENV_VARIABLE = "AWS_REGION";
    public static final String DEFAULT_EVENT_BUS = "default";
    public static final String EVENT_BUS_NAME = setupEventBus();

    public static final Region AWS_REGION = setupRegion();

    private ApplicationConstants() {

    }

    @JacocoGenerated
    private static String setupEventBus() {
        return ENVIRONMENT.readEnvOpt(EVENT_BUS_ENV_VARIABLE).orElse(DEFAULT_EVENT_BUS);
    }

    @JacocoGenerated
    private static Region setupRegion() {
        return ENVIRONMENT.readEnvOpt(AWS_REGION_ENV_VARIABLE).map(Region::of).orElse(Region.EU_WEST_1);
    }

    @JacocoGenerated
    public static S3Client defaultS3Client() {
        return S3Client.builder()
                   .region(ApplicationConstants.AWS_REGION)
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    @JacocoGenerated
    public static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
                   .region(ApplicationConstants.AWS_REGION)
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }
}
