package no.unit.nva.publication.update;

import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.publication.AccessRight;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class UpdatePublicationHandler extends ApiGatewayHandler<UpdatePublicationRequest, PublicationResponse> {

    public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE = "Identifiers in path and in body, do not match";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public UpdatePublicationHandler() {
        this(new ResourceService(
                 AmazonDynamoDBClientBuilder.defaultClient(),
                 Clock.systemDefaultZone()),
             TicketService.defaultService(),
             new Environment(),
             IdentityServiceClient.prepare());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     * @param environment     environment
     */
    public UpdatePublicationHandler(ResourceService resourceService,
                                    TicketService ticketService,
                                    Environment environment,
                                    IdentityServiceClient identityServiceClient) {
        super(UpdatePublicationRequest.class, environment);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
    }

    @Override
    protected PublicationResponse processInput(UpdatePublicationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        SortableIdentifier identifierInPath = RequestUtil.getIdentifier(requestInfo);
        validateRequest(identifierInPath, input);
        Publication existingPublication = fetchExistingPublication(requestInfo, identifierInPath);
        Publication publicationUpdate = input.generatePublicationUpdate(existingPublication);
        if (isAlreadyPublished(existingPublication)) {
            createPublishingRequestOnFileUpdate(publicationUpdate);
        }
        Publication updatedPublication = resourceService.updatePublication(publicationUpdate);
        return PublicationResponse.fromPublication(updatedPublication);
    }

    @Override
    protected Integer getSuccessStatusCode(UpdatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_OK;
    }

    private static boolean isAlreadyPublished(Publication existingPublication) {
        return PublicationStatus.PUBLISHED.equals(existingPublication.getStatus())
               || PublicationStatus.PUBLISHED_METADATA.equals(existingPublication.getStatus());
    }

    private void createPublishingRequestOnFileUpdate(Publication publicationUpdate) throws ApiGatewayException {
        if (containsNewFiles(publicationUpdate)) {
            TicketEntry.requestNewTicket(publicationUpdate, PublishingRequestCase.class)
                .persistNewTicket(ticketService);
        }
    }

    private boolean containsNewFiles(Publication publicationUpdate) {
        return !getUnpublishedFiles(publicationUpdate).isEmpty();
    }

    private List<AssociatedArtifact> getUnpublishedFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream()
                   .filter(this::isUnpublishedFile)
                   .collect(Collectors.toList());
    }

    private boolean isUnpublishedFile(AssociatedArtifact artifact) {
        return artifact instanceof UnpublishedFile;
    }

    private Publication fetchExistingPublication(RequestInfo requestInfo,
                                                 SortableIdentifier identifierInPath) throws ApiGatewayException {
        UserInstance userInstance = createUserInstanceFromRequest(requestInfo);

        return userCanEditOtherPeoplesPublications(requestInfo)
                   ? fetchPublicationForPrivilegedUser(identifierInPath, userInstance)
                   : fetchPublicationForPublicationOwner(identifierInPath, userInstance);
    }

    private UserInstance createUserInstanceFromRequest(RequestInfo requestInfo) throws ApiGatewayException {
        return requestInfo.clientIsThirdParty()
                   ? createExternalUserInstance(requestInfo, identityServiceClient)
                   : extractUserInstance(requestInfo);
    }

    private UserInstance extractUserInstance(RequestInfo requestInfo) throws UnauthorizedException {
        return attempt(requestInfo::getCurrentCustomer)
                   .map(customerId -> UserInstance.create(requestInfo.getNvaUsername(), customerId))
                   .orElseThrow(fail -> new UnauthorizedException());
    }

    private Publication fetchPublicationForPublicationOwner(SortableIdentifier identifierInPath,
                                                            UserInstance userInstance)
        throws ApiGatewayException {
        return resourceService.getPublication(userInstance, identifierInPath);
    }

    private Publication fetchPublicationForPrivilegedUser(SortableIdentifier identifierInPath,
                                                          UserInstance userInstance)
        throws NotFoundException, NotAuthorizedException {
        Publication existingPublication;
        existingPublication = resourceService.getPublicationByIdentifier(identifierInPath);
        checkUserIsInSameInstitutionAsThePublication(userInstance, existingPublication);
        return existingPublication;
    }

    private void checkUserIsInSameInstitutionAsThePublication(UserInstance userInstance,
                                                              Publication existingPublication)
        throws NotAuthorizedException {
        if (!userInstance.getOrganizationUri().equals(existingPublication.getPublisher().getId())) {
            throw new NotAuthorizedException();
        }
    }

    private boolean userCanEditOtherPeoplesPublications(RequestInfo requestInfo) {

        var accessRight = AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.toString();
        return !requestInfo.clientIsThirdParty() && requestInfo.userIsAuthorized(accessRight);
    }

    private void validateRequest(SortableIdentifier identifierInPath, UpdatePublicationRequest input)
        throws BadRequestException {
        if (identifiersDoNotMatch(identifierInPath, input)) {
            throw new BadRequestException(IDENTIFIER_MISMATCH_ERROR_MESSAGE);
        }
    }

    private boolean identifiersDoNotMatch(SortableIdentifier identifierInPath,
                                          UpdatePublicationRequest input) {
        return !identifierInPath.equals(input.getIdentifier());
    }
}