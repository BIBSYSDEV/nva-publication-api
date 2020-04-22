package no.unit.nva.publication;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.publication.exception.InputException;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.RequestInfo;
import org.apache.http.HttpHeaders;

import java.util.Optional;
import java.util.UUID;

public class RequestUtil {

    public static final String IDENTIFIER = "identifier";
    public static final String MISSING_AUTHORIZATION_IN_HEADERS = "Missing Authorization in Headers";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";

    public static final String REQUEST_CONTEXT_AUTHORIZER_CLAIMS = "/requestContext/authorizer/claims/";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";
    public static final String MISSING_CLAIM_IN_REQUEST_CONTEXT =
            "Missing claim in requestContext: ";

    public static String getAuthorization(RequestInfo requestInfo) throws ApiGatewayException {
        try {
            return requestInfo.getHeaders().get(HttpHeaders.AUTHORIZATION);
        } catch (Exception e) {
            throw new InputException(MISSING_AUTHORIZATION_IN_HEADERS, e);
        }
    }

    public static UUID getIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        String identifier = null;
        try {
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            return UUID.fromString(identifier);
        } catch (Exception e) {
            throw new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }

    public static String getOrgNumber(RequestInfo requestInfo) {
        requestInfo.get
    }

    public static String getOwner(RequestInfo requestInfo) {
    }

}
