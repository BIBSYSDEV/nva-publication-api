package no.unit.nva.doi;

import static java.util.Objects.nonNull;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class ReserveDoiRequestValidator {

    public static final String NOT_DRAFT_STATUS_ERROR_MESSAGE = "Operation is not allowed, publication is not a draft";
    public static final String DOI_ALREADY_EXISTS_ERROR_MESSAGE = "Operation is not allowed, publication already has "
                                                                  + "doi";
    public static final String UNSUPPORTED_ROLE_ERROR_MESSAGE = "Only owner can reserve a doi";

    private ReserveDoiRequestValidator() {
    }

    public static void validateRequest(RequestInfo requestInfo, Publication publication) throws ApiGatewayException {
        validateReserveDoiRequest(requestInfo, publication);
    }

    private static boolean userIsNotOwnerOfPublication(RequestInfo requestInfo, Publication publication)
        throws ApiGatewayException {
        return !RequestUtil.getOwner(requestInfo).equals(publication.getResourceOwner().getOwner());
    }

    private static boolean isNotADraft(Publication publication) {
        return !PublicationStatus.DRAFT.equals(publication.getStatus());
    }

    private static void validateReserveDoiRequest(RequestInfo requestInfo, Publication publication) throws ApiGatewayException {
        if (alreadyHasDoi(publication)) {
            throw new BadMethodException(DOI_ALREADY_EXISTS_ERROR_MESSAGE);
        }
        if (isNotADraft(publication)) {
            throw new BadMethodException(NOT_DRAFT_STATUS_ERROR_MESSAGE);
        }
        if (userIsNotOwnerOfPublication(requestInfo, publication)) {
            throw new UnauthorizedException(UNSUPPORTED_ROLE_ERROR_MESSAGE);
        }
    }

    private static boolean alreadyHasDoi(Publication publication) {
        return nonNull(publication.getDoi());
    }
}