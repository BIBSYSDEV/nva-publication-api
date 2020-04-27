package no.unit.nva.publication;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.publication.exception.InputException;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.RequestInfo;
import org.apache.http.HttpHeaders;

import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtil {

    public static final String IDENTIFIER = "identifier";
    public static final String MISSING_AUTHORIZATION_IN_HEADERS = "Missing Authorization in Headers";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    public static final String AUTHORIZER_CLAIMS = "/authorizer/claims/";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";
    public static final String MISSING_CLAIM_IN_REQUEST_CONTEXT =
            "Missing claim in requestContext: ";

    private static final Logger logger= LoggerFactory.getLogger(RequestUtil.class);
    public static final String AUTHORIZATION_SUCCESSFUL = "Authorization successful";

    private RequestUtil() {
    }

    /**
     * Get Authorization header from request.
     *
     * @param requestInfo   requestInfo
     * @return  value of Authorization header
     * @throws ApiGatewayException  exception thrown if value is missing
     */
    public static String getAuthorization(RequestInfo requestInfo) throws ApiGatewayException {
        try {
            String authorization = requestInfo.getHeaders().get(HttpHeaders.AUTHORIZATION);
            logger.debug(AUTHORIZATION_SUCCESSFUL);
            Objects.requireNonNull(authorization);
            return authorization;
        } catch (Exception e) {
            throw new InputException(MISSING_AUTHORIZATION_IN_HEADERS, e);
        }
    }

    /**
     * Get identifier from request path parameters.
     *
     * @param requestInfo   requestInfo
     * @return  the identifier
     * @throws ApiGatewayException  exception thrown if value is missing
     */
    public static UUID getIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        String identifier = null;
        try {
            logger.info("Trying to read Publication identifier...");
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            logger.info("Requesting publication metadata for ID:"+identifier);
            return UUID.fromString(identifier);
        } catch (Exception e) {
            throw new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }

    /**
     * Get orgNumber from requestContext authorizer claims.
     *
     * @param requestInfo   requestInfo.
     * @return  the orgNumber
     * @throws ApiGatewayException  exception thrown if value is missing
     */
    public static String getOrgNumber(RequestInfo requestInfo) throws ApiGatewayException {
        if (requestInfo.getRequestContext() != null) {
            JsonNode jsonNode = requestInfo.getRequestContext().at(AUTHORIZER_CLAIMS + CUSTOM_ORG_NUMBER);
            if (!jsonNode.isMissingNode()) {
                return jsonNode.textValue();
            }
        }
        throw new InputException(MISSING_CLAIM_IN_REQUEST_CONTEXT + CUSTOM_ORG_NUMBER, null);
    }

    /**
     * Get owner from requestContext authorizer claims.
     *
     * @param requestInfo   requestInfo.
     * @return  the owner
     * @throws ApiGatewayException  exception thrown if value is missing
     */
    public static String getOwner(RequestInfo requestInfo) throws ApiGatewayException {
        if (requestInfo.getRequestContext() != null) {
            JsonNode jsonNode = requestInfo.getRequestContext().at(AUTHORIZER_CLAIMS + CUSTOM_FEIDE_ID);
            if (!jsonNode.isMissingNode()) {
                return jsonNode.textValue();
            }
        }
        throw new InputException(MISSING_CLAIM_IN_REQUEST_CONTEXT + CUSTOM_FEIDE_ID, null);
    }

}
