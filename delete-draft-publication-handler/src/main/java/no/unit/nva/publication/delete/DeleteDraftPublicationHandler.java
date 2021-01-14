package no.unit.nva.publication.delete;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.DeletePublicationEvent;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import static nva.commons.core.JsonUtils.objectMapper;

public class DeleteDraftPublicationHandler extends DestinationsEventBridgeEventHandler<DeletePublicationEvent, Void> {

    public static final String DELETE_WITH_DOI_ERROR = "Not allowed to delete Draft Publication with DOI. "
            + "Remove DOI first and try again";
    private final PublicationService publicationService;

    /**
     * Default constructor for DeleteDraftPublicationHandler.
     */
    @JacocoGenerated
    public DeleteDraftPublicationHandler() {
        this(new DynamoDBPublicationService(
                        AmazonDynamoDBClientBuilder.defaultClient(),
                        objectMapper,
                        new Environment())
        );
    }

    /**
     * Constructor for DeleteDraftPublicationHandler.
     *
     * @param publicationService    publicationService
     */
    public DeleteDraftPublicationHandler(PublicationService publicationService) {
        super(DeletePublicationEvent.class);
        this.publicationService = publicationService;

    }

    @Override
    protected Void processInputPayload(
            DeletePublicationEvent input,
            AwsEventBridgeEvent<AwsEventBridgeDetail<DeletePublicationEvent>> event,
            Context context) {
        if (input.hasDoi()) {
            throw new RuntimeException(DELETE_WITH_DOI_ERROR);
        }
        try {
            publicationService.deleteDraftPublication(input.getIdentifier());
        } catch (ApiGatewayException e) {
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }
}
