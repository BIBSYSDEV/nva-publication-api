package no.unit.nva.publication.delete;

import static no.unit.nva.model.PublicationOperation.DELETE;
import static no.unit.nva.publication.RequestUtil.createUserInstanceFromRequest;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class DeletePublicationHandler extends ApiGatewayHandler<Void, Void> {

    public static final String LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS =
        "Lambda Function Invocation Result - Success";
    public static final String NVA_PUBLICATION_DELETE_SOURCE = "nva.publication.delete";
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;
    private UriRetriever uriRetriever;

    /**
     * Default constructor for DeletePublicationHandler.
     */
    @JacocoGenerated
    public DeletePublicationHandler() {
        this(ResourceService.defaultService(), new Environment(), IdentityServiceClient.prepare(), UriRetriever.defaultUriRetriever());
    }

    /**
     * Constructor for DeletePublicationHandler.
     *
     * @param resourceService   resourceService
     * @param environment       environment
     */
    public DeletePublicationHandler(ResourceService resourceService,
                                    Environment environment,
                                    IdentityServiceClient identityServiceClient,
                                    UriRetriever uriRetriever) {
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.identityServiceClient = identityServiceClient;
        this.uriRetriever = uriRetriever;
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var userInstance = createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var publicationIdentifier = RequestUtil.getIdentifier(requestInfo);

        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);


        if (publication.getStatus() == PublicationStatus.DRAFT) {
            PublicationPermissionStrategy.create(publication, userInstance, uriRetriever).authorize(DELETE);
            handleDraftDeletion(userInstance, publicationIdentifier);
        } else {
            unsupportedPublicationForDeletion(publication);
        }

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }

    private static void unsupportedPublicationForDeletion(Publication publication) throws BadRequestException {
        throw new BadRequestException(
            String.format("Publication status %s is not supported for deletion", publication.getStatus()));
    }

    private void handleDraftDeletion(UserInstance userInstance, SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        resourceService.markPublicationForDeletion(userInstance, publicationIdentifier);
    }
}
