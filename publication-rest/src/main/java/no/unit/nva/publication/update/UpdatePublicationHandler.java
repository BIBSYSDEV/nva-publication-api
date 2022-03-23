package no.unit.nva.publication.update;

import static no.unit.nva.publication.PublicationServiceConfig.EXTERNAL_SERVICES_HTTP_CLIENT;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.AccessRight;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class UpdatePublicationHandler extends ApiGatewayHandler<UpdatePublicationRequest, PublicationResponse> {

    public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE = "Identifiers in path and in body, do not match";
    private final ResourceService resourceService;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public UpdatePublicationHandler() {
        this(new ResourceService(
                 AmazonDynamoDBClientBuilder.defaultClient(),
                 EXTERNAL_SERVICES_HTTP_CLIENT,
                 Clock.systemDefaultZone()),
             new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     * @param environment     environment
     */
    public UpdatePublicationHandler(ResourceService resourceService,
                                    Environment environment) {
        super(UpdatePublicationRequest.class, environment);
        this.resourceService = resourceService;
    }

    @Override
    protected PublicationResponse processInput(UpdatePublicationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        SortableIdentifier identifierInPath = RequestUtil.getIdentifier(requestInfo);
        validateRequest(identifierInPath, input);
        Publication existingPublication = fetchExistingPublication(requestInfo, identifierInPath);
        Publication publicationUpdate = input.generatePublicationUpdate(existingPublication);
        Publication updatedPublication = resourceService.updatePublication(publicationUpdate);
        return PublicationResponse.fromPublication(updatedPublication);
    }

    @Override
    protected Integer getSuccessStatusCode(UpdatePublicationRequest input, PublicationResponse output) {
        return HttpStatus.SC_OK;
    }

    private Publication fetchExistingPublication(RequestInfo requestInfo,
                                                 SortableIdentifier identifierInPath) throws ApiGatewayException {
        UserInstance userInstance = RequestUtil.extractUserInstance(requestInfo);

        return userCanEditOtherPeoplesPublications(requestInfo)
                   ? fetchPublicationForPrivilegedUser(identifierInPath, userInstance)
                   : fetchPublicationForPublicationOwner(identifierInPath, userInstance);
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
        return requestInfo.userIsAuthorized(accessRight) ;

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