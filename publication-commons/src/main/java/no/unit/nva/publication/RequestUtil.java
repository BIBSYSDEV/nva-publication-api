package no.unit.nva.publication;

import static java.lang.Integer.parseInt;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.InputException;
import no.unit.nva.publication.service.impl.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtil {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    public static final String PAGESIZE_IS_NOT_A_VALID_POSITIVE_INTEGER = "pageSize is not a valid positive integer: ";
    public static final String AUTHORIZER_CLAIMS = "/authorizer/claims/";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_CUSTOMER_ID = "custom:customerId";
    public static final String MISSING_CLAIM_IN_REQUEST_CONTEXT =
        "Missing claim in requestContext: ";
    public static final String PAGESIZE = "pagesize";
    public static final int DEFAULT_PAGESIZE = 10;


    private static final Logger logger = LoggerFactory.getLogger(RequestUtil.class);
    public static final String USING_DEFAULT_VALUE = ", using default value: ";

    private RequestUtil() {
    }

    /**
     * Get identifier from request path parameters.
     *
     * @param requestInfo requestInfo
     * @return the identifier
     * @throws ApiGatewayException exception thrown if value is missing
     */
    public static SortableIdentifier getIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        String identifier = null;
        try {
            logger.info("Trying to read Publication identifier...");
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            logger.info("Requesting publication metadata for ID:" + identifier);
            return new SortableIdentifier(identifier);
        } catch (Exception e) {
            throw new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }

    /**
     * Get customerId from requestContext authorizer claims.
     *
     * @param requestInfo requestInfo.
     * @return the customerId
     * @throws ApiGatewayException exception thrown if value is missing
     */
    public static URI getCustomerId(RequestInfo requestInfo) throws ApiGatewayException {
        JsonNode jsonNode = requestInfo.getRequestContext().at(AUTHORIZER_CLAIMS + CUSTOM_CUSTOMER_ID);
        if (!jsonNode.isMissingNode()) {
            return URI.create(jsonNode.textValue());
        }
        throw new InputException(MISSING_CLAIM_IN_REQUEST_CONTEXT + CUSTOM_CUSTOMER_ID, null);
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
     * Get pagesize from request query parameters.
     *
     * @param requestInfo requestInfo
     * @return the pagesize if given, otherwise DEFAULT_PAGESIZE
     * @throws ApiGatewayException exception thrown if value is not legal positive integer
     */
    public static int getPageSize(RequestInfo requestInfo) throws ApiGatewayException {
        String pagesizeString = null;
        try {
            logger.debug("Trying to read pagesize...");
            pagesizeString = requestInfo.getQueryParameters().get(PAGESIZE);
            if (!Strings.isEmpty(pagesizeString)) {
                logger.debug("got pagesize='" + pagesizeString + "'");
                int pageSize = parseInt(pagesizeString);
                if (pageSize > 0) {
                    return  pageSize;
                } else {
                    throw new InputException(PAGESIZE_IS_NOT_A_VALID_POSITIVE_INTEGER + pagesizeString, null);
                }
            } else {
                logger.debug(USING_DEFAULT_VALUE + DEFAULT_PAGESIZE);
                return DEFAULT_PAGESIZE;
            }
        } catch (Exception e) {
            throw new InputException(PAGESIZE_IS_NOT_A_VALID_POSITIVE_INTEGER + pagesizeString, e);
        }
    }

    public static UserInstance extractUserInstance(RequestInfo requestInfo) {
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        String useIdentifier = requestInfo.getFeideId().orElse(null);
        return new UserInstance(useIdentifier, customerId);
    }
}
