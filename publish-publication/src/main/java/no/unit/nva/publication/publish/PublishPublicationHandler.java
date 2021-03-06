package no.unit.nva.publication.publish;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class PublishPublicationHandler extends ApiGatewayHandler<Void, PublishPublicationStatusResponse> {

    public static final String LOCATION_TEMPLATE = "%s://%s/publication/%s";
    public static final String API_SCHEME = "API_SCHEME";
    public static final String API_HOST = "API_HOST";
    private final String apiScheme;
    private final String apiHost;
    private final ResourceService resourceService;

    /**
     * Default constructor for PublishPublicationHandler.
     */
    @JacocoGenerated
    public PublishPublicationHandler() {
        this(
            new Environment(),
            new ResourceService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                Clock.systemDefaultZone()
            )
        );
    }

    /**
     * Constructor for PublishPublicationHandler.
     *
     * @param environment     environment reader
     * @param resourceService publicationService
     */
    public PublishPublicationHandler(Environment environment, ResourceService resourceService) {
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.apiScheme = environment.readEnv(API_SCHEME);
        this.apiHost = environment.readEnv(API_HOST);
    }

    @Override
    protected PublishPublicationStatusResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        String user = requestInfo.getFeideId().orElse(null);
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        UserInstance userInstance = new UserInstance(user, customerId);
        setAdditionalHeadersSupplier(() -> Map.of(HttpHeaders.LOCATION, getLocation(identifier).toString()));

        return resourceService.publishPublication(userInstance, identifier);
    }

    protected URI getLocation(SortableIdentifier identifier) {
        return URI.create(String.format(LOCATION_TEMPLATE, apiScheme, apiHost, identifier.toString()));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublishPublicationStatusResponse output) {
        return output.getStatusCode();
    }
}
