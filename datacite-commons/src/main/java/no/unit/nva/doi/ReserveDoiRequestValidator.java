package no.unit.nva.doi;

import static java.util.Objects.nonNull;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class ReserveDoiRequestValidator {

    public static final String NOT_DRAFT_STATUS_ERROR_MESSAGE = "Operation is not allowed, publication is not a draft";
    public static final String DOI_ALREADY_EXISTS_ERROR_MESSAGE =
        "Operation is not allowed, publication already has " + "doi";
    public static final String UNSUPPORTED_ROLE_ERROR_MESSAGE = "User does not have rights to create doi";

    private ReserveDoiRequestValidator() {
    }

    public static void validateRequest(UserInstance userInstance, Resource resource) throws ApiGatewayException {
        validateReserveDoiRequest(userInstance, resource);
    }

    private static boolean userHasNoRightsToCreateDoi(UserInstance userInstance, Resource resource) {
        return !PublicationPermissions.create(resource.toPublication(), userInstance)
                    .allowsAction(PublicationOperation.DOI_REQUEST_CREATE);
    }

    private static boolean isNotADraft(Resource resource) {
        return !PublicationStatus.DRAFT.equals(resource.getStatus());
    }

    private static void validateReserveDoiRequest(UserInstance userInstance, Resource resource)
        throws ApiGatewayException {
        if (alreadyHasDoi(resource)) {
            throw new BadMethodException(DOI_ALREADY_EXISTS_ERROR_MESSAGE);
        }
        if (isNotADraft(resource)) {
            throw new BadMethodException(NOT_DRAFT_STATUS_ERROR_MESSAGE);
        }
        if (userHasNoRightsToCreateDoi(userInstance, resource)) {
            throw new UnauthorizedException(UNSUPPORTED_ROLE_ERROR_MESSAGE);
        }
    }

    private static boolean alreadyHasDoi(Resource resource) {
        return nonNull(resource.getDoi());
    }
}