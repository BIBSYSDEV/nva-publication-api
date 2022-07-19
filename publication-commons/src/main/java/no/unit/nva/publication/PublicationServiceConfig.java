package no.unit.nva.publication;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Clock;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public final class PublicationServiceConfig {
    
    public static final Environment ENVIRONMENT = new Environment();
    public static final String URI_EMPTY_FRAGMENT = null;
    public static final String PATH_SEPARATOR = "/";
    @Deprecated
    public static final String MESSAGE_PATH = "/messages";
    public static final String PUBLICATION_PATH = "/publication";
    public static final String SUPPORT_CASE_PATH = "support-case";
    public static final String PUBLICATION_IDENTIFIER_PATH_PARAMETER = "publicationIdentifier";
    public static final String API_HOST = ENVIRONMENT.readEnv("API_HOST");
    @Deprecated
    public static final String ID_NAMESPACE = ENVIRONMENT.readEnv("ID_NAMESPACE");
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");
    public static final AmazonDynamoDB DEFAULT_DYNAMODB_CLIENT = defaultDynamoDbClient();
    public static final String API_SCHEME = "https";
    public static final HttpClient EXTERNAL_SERVICES_HTTP_CLIENT = HttpClient.newBuilder().build();
    public static final ObjectMapper dtoObjectMapper = JsonUtils.dtoObjectMapper;
    
    private PublicationServiceConfig() {
    
    }
    
    @JacocoGenerated
    public static ResourceService defaultResourceService() {
        return new ResourceService(defaultDynamoDbClient(), Clock.systemDefaultZone());
    }
    
    @JacocoGenerated
    public static AmazonDynamoDB defaultDynamoDbClient() {
        return AmazonDynamoDBClientBuilder
            .standard()
            .withRegion(AWS_REGION)
            .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
            .build();
    }
}
