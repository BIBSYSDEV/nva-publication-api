package no.unit.nva.publication;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.publication.exception.InputException;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.RequestInfo;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtil {

    public static final String IDENTIFIER = "identifier";
    public static final String MISSING_AUTHORIZATION_IN_HEADERS = "Missing Authorization in Headers";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    public static final String PAGESIZE_IS_NOT_A_VALID_POSITIVE_INTEGER = "pageSize is not a valid positive integer: ";
    public static final String AUTHORIZER_CLAIMS = "/authorizer/claims/";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";
    public static final String MISSING_CLAIM_IN_REQUEST_CONTEXT =
        "Missing claim in requestContext: ";
    public static final String LAST_KEY = "lastkey";
    public static final String PAGESIZE = "pagesize";
    public static final int DEFAULT_PAGESIZE = 10;


    private static final Logger logger = LoggerFactory.getLogger(RequestUtil.class);

    private RequestUtil() {
    }

    /**
     * Get identifier from request path parameters.
     *
     * @param requestInfo requestInfo
     * @return the identifier
     * @throws ApiGatewayException exception thrown if value is missing
     */
    public static UUID getIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        String identifier = null;
        try {
            logger.info("Trying to read Publication identifier...");
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            logger.info("Requesting publication metadata for ID:" + identifier);
            return UUID.fromString(identifier);
        } catch (Exception e) {
            throw new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }

    /**
     * Get orgNumber from requestContext authorizer claims.
     *
     * @param requestInfo requestInfo.
     * @return the orgNumber
     * @throws ApiGatewayException exception thrown if value is missing
     */
    public static String getOrgNumber(RequestInfo requestInfo) throws ApiGatewayException {
        JsonNode jsonNode = requestInfo.getRequestContext().at(AUTHORIZER_CLAIMS + CUSTOM_ORG_NUMBER);
        if (!jsonNode.isMissingNode()) {
            return jsonNode.textValue();
        }
        throw new InputException(MISSING_CLAIM_IN_REQUEST_CONTEXT + CUSTOM_ORG_NUMBER, null);
    }

    /**
     * Get owner from requestContext authorizer claims.
     *
     * @param requestInfo requestInfo.
     * @return the owner
     * @throws ApiGatewayException exception thrown if value is missing
     */
    public static String getOwner(RequestInfo requestInfo) throws ApiGatewayException {
        JsonNode jsonNode = requestInfo.getRequestContext().at(AUTHORIZER_CLAIMS + CUSTOM_FEIDE_ID);
        if (!jsonNode.isMissingNode()) {
            return jsonNode.textValue();
        }
        throw new InputException(MISSING_CLAIM_IN_REQUEST_CONTEXT + CUSTOM_FEIDE_ID, null);
    }

    /**
     * Get pageSize from request query parameters.
     *
     * @param requestInfo requestInfo
     * @return the pageSize ig given, otherwise DEFAULT_PAGESIZE
     * @throws ApiGatewayException exception thrown if value is not legal positive integer
     */
    public static int getPageSize(RequestInfo requestInfo) throws ApiGatewayException {
        String pagesizeString = null;
        try {
            logger.debug("Trying to read pagesize...");
            pagesizeString = requestInfo.getPathParameters().get(IDENTIFIER);
            if (!Strings.isEmpty(pagesizeString)) {
                logger.debug("got pagesize:" + pagesizeString);
                return Integer.getInteger(pagesizeString);
            } else {
                return DEFAULT_PAGESIZE;
            }
        } catch (Exception e) {
            throw new InputException(PAGESIZE_IS_NOT_A_VALID_POSITIVE_INTEGER + pagesizeString, e);
        }
    }

    /**
     * Get lastKey from request query parameters.
     *
     * @param requestInfo requestInfo
     * @return the lastkey if given
     */
    public static Map<String, AttributeValue> getLastKey(RequestInfo requestInfo) throws ApiGatewayException {
        String result;
        logger.debug("Trying to read lastKey...");
        String lastKeyEncoded = requestInfo.getPathParameters().get(LAST_KEY);
        Map<String, AttributeValue>  lastKey = null;
//        if (!Strings.isEmpty(lastKey)) {
//            logger.debug("got lastKey:" + lastKey);
//            result = Optional.of(lastKey);
//        } else {
//            result = Optional.empty();
//        }
        return lastKey;
    }

}
