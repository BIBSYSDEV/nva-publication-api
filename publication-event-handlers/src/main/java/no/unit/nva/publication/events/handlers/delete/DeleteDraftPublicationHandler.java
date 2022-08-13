package no.unit.nva.publication.events.handlers.delete;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.bodies.ResourceDraftedForDeletionEvent;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;

import java.time.Clock;

import static nva.commons.core.attempt.Try.attempt;

public class DeleteDraftPublicationHandler
    extends DestinationsEventBridgeEventHandler<ResourceDraftedForDeletionEvent, Void> {
    
    public static final String DELETE_WITH_DOI_ERROR = "Not allowed to delete Draft Publication with DOI. "
                                                       + "Remove DOI first and try again";
    private final ResourceService resourceService;
    
    /**
     * Default constructor for DeleteDraftPublicationHandler.
     */
    @JacocoGenerated
    public DeleteDraftPublicationHandler() {
        this(new ResourceService(
            AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone())
        );
    }
    
    /**
     * Constructor for DeleteDraftPublicationHandler.
     *
     * @param resourceService publicationService
     */
    public DeleteDraftPublicationHandler(ResourceService resourceService) {
        super(ResourceDraftedForDeletionEvent.class);
        this.resourceService = resourceService;
    }
    
    @Override
    protected Void processInputPayload(
        ResourceDraftedForDeletionEvent input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<ResourceDraftedForDeletionEvent>> event,
        Context context) {
        verifyPublicationCanBeDeleted(input);
        return attempt(() -> deleteDraftPublicationForUser(input)).orElseThrow(this::handleException);
    }

    private void verifyPublicationCanBeDeleted(ResourceDraftedForDeletionEvent input) {
        if (input.hasDoi()) {
            throwPublicationHasDoiError();
        }
    }

    private RuntimeException handleException(Failure<Void> fail) {
        return new RuntimeException(fail.getException());
    }

    private Void deleteDraftPublicationForUser(ResourceDraftedForDeletionEvent input) throws ApiGatewayException {
        var userInstance = fetchUserInformationForPublication(input);
        resourceService.deleteDraftPublication(userInstance, input.getIdentifier());
        return null;
    }

    private UserInstance fetchUserInformationForPublication(ResourceDraftedForDeletionEvent input)
        throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(input.getIdentifier());
        return UserInstance.fromPublication(publication);
    }
    
    private void throwPublicationHasDoiError() {
        throw new RuntimeException(DELETE_WITH_DOI_ERROR);
    }
}
