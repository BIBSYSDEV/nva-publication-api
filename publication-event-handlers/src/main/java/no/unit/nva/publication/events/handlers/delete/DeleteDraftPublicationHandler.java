package no.unit.nva.publication.events.handlers.delete;

import static java.util.Objects.nonNull;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.bodies.ResourceDraftedForDeletionEvent;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

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
        if (input.hasDoi()) {
            throwPublicationHasDoiError();
        }
        
        try {
            var userInstance = fetchUserInformationForPublication(input);
            resourceService.deleteDraftPublication(userInstance, input.getIdentifier());
        } catch (NotFoundException | BadRequestException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    
    private UserInstance fetchUserInformationForPublication(ResourceDraftedForDeletionEvent input)
        throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(input.getIdentifier());
        if (nonNull(publication.getDoi())) {
            throwPublicationHasDoiError();
        }
        var value = publication.getResourceOwner().getOwner().getValue();
        var publisherId = publication.getPublisher().getId();
        return UserInstance.create(value, publisherId);
    }
    
    private void throwPublicationHasDoiError() {
        throw new RuntimeException(DELETE_WITH_DOI_ERROR);
    }
}
