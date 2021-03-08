package no.unit.nva.publication.delete;

import static java.util.Objects.nonNull;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.DeletePublicationEvent;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class DeleteDraftPublicationHandler extends DestinationsEventBridgeEventHandler<DeletePublicationEvent, Void> {

    public static final String DELETE_WITH_DOI_ERROR = "Not allowed to delete Draft Publication with DOI. "
                                                       + "Remove DOI first and try again";
    private final ResourceService resourceService;

    /**
     * Default constructor for DeleteDraftPublicationHandler.
     */
    @JacocoGenerated
    public DeleteDraftPublicationHandler() {
        this(new ResourceService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            Clock.systemDefaultZone())
        );
    }

    /**
     * Constructor for DeleteDraftPublicationHandler.
     *
     * @param resourceService publicationService
     */
    public DeleteDraftPublicationHandler(ResourceService resourceService) {
        super(DeletePublicationEvent.class);
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInputPayload(
        DeletePublicationEvent input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<DeletePublicationEvent>> event,
        Context context) {
        if (input.hasDoi()) {
            throwPublicationHasDoiError();
        }

        try {
            UserInstance userInstance = fetchUserInformationForPublication(input);
            resourceService.deleteDraftPublication(userInstance, input.getIdentifier());
        } catch (NotFoundException | BadRequestException | TransactionFailedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private UserInstance fetchUserInformationForPublication(DeletePublicationEvent input) throws NotFoundException {
        Publication publication = resourceService.getPublicationByIdentifier(input.getIdentifier());
        if (nonNull(publication.getDoi())) {
            throwPublicationHasDoiError();
        }
        return new UserInstance(publication.getOwner(),publication.getPublisher().getId());
    }

    private void throwPublicationHasDoiError() {
        throw new RuntimeException(DELETE_WITH_DOI_ERROR);
    }
}
