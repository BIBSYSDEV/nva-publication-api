package no.unit.nva.publication.delete;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class DeletePublicationHandler extends ApiGatewayHandler<Void, Void> {

    private final ResourceService resourceService;

    /**
     * Default constructor for DeletePublicationHandler.
     */
    @JacocoGenerated
    public DeletePublicationHandler() {
        this(defaultService(), new Environment());
    }

    /**
     * Constructor for DeletePublicationHandler.
     *
     * @param resourceService resourceService
     * @param environment     environment
     */
    public DeletePublicationHandler(ResourceService resourceService, Environment environment) {
        super(Void.class, environment);
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        SortableIdentifier identifier = RequestUtil.getIdentifier(requestInfo);
        String owner = requestInfo.getNvaUsername();
        URI customerId = requestInfo.getCurrentCustomer();
        UserInstance userInstance = UserInstance.create(owner, customerId);

        resourceService.markPublicationForDeletion(userInstance, identifier);

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }

    @JacocoGenerated
    private static ResourceService defaultService() {
        return new ResourceService(AmazonDynamoDBClientBuilder.defaultClient(),Clock.systemDefaultZone());
    }
}
