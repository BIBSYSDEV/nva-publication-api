package no.unit.nva.publication.publish;

import static nva.commons.core.JsonUtils.objectMapper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpHeaders;
import org.slf4j.LoggerFactory;

public class PublishPublicationHandler extends ApiGatewayHandler<Void, PublishPublicationStatusResponse> {

    public static final String LOCATION_TEMPLATE = "%s://%s/publication/%s";
    public static final String API_SCHEME = "API_SCHEME";
    public static final String API_HOST = "API_HOST";
    private String apiScheme;
    private String apiHost;
    private PublicationService publicationService;

    /**
     * Default constructor for PublishPublicationHandler.
     */
    @JacocoGenerated
    public PublishPublicationHandler() {
        this(
            new Environment(),
            new DynamoDBPublicationService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                objectMapper,
                new Environment())
        );
    }

    /**
     * Constructor for PublishPublicationHandler.
     *
     * @param environment        environment reader
     * @param publicationService publicationService
     */
    public PublishPublicationHandler(Environment environment, PublicationService publicationService) {
        super(Void.class, environment, LoggerFactory.getLogger(PublishPublicationHandler.class));
        this.publicationService = publicationService;
        this.apiScheme = environment.readEnv(API_SCHEME);
        this.apiHost = environment.readEnv(API_HOST);
    }

    @Override
    protected PublishPublicationStatusResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UUID identifier = RequestUtil.getIdentifier(requestInfo);
        setAdditionalHeadersSupplier(() -> Map.of(HttpHeaders.LOCATION, getLocation(identifier).toString()));
        return publicationService.publishPublication(identifier);
    }

    protected URI getLocation(UUID identifier) {
        return URI.create(String.format(LOCATION_TEMPLATE, apiScheme, apiHost, identifier));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublishPublicationStatusResponse output) {
        return output.getStatusCode();
    }
}
