package no.unit.nva.publication;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.exception.BadRequestException;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtil {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    public static final String PAGESIZE_IS_NOT_A_VALID_POSITIVE_INTEGER = "pageSize is not a valid positive integer: ";

    public static final String CURRENT_CUSTOMER = "custom:customerId";
    public static final String MISSING_CLAIM_IN_REQUEST_CONTEXT =
        "Missing claim in requestContext: ";
    public static final String PAGESIZE = "pagesize";
    public static final int DEFAULT_PAGESIZE = 10;
    public static final String USING_DEFAULT_VALUE = ", using default value: ";
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
    public static SortableIdentifier getIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        String identifier = null;
        try {
            logger.info("Trying to read Publication identifier...");
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            logger.info("Requesting publication metadata for ID:" + identifier);
            return new SortableIdentifier(identifier);
        } catch (Exception e) {
            throw new BadRequestException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }

    /**
     * Get owner from requestContext authorizer claims.
     *
     * @param requestInfo requestInfo.
     * @return the owner
     * @throws ApiGatewayException exception thrown if value is missing
     */
    public static String getOwner(RequestInfo requestInfo) throws ApiGatewayException {
        return attempt(requestInfo::getNvaUsername)
            .toOptional(fail -> logger.warn("Could not authenticate user", fail.getException()))
            .orElseThrow(UnauthorizedException::new);
    }
}
