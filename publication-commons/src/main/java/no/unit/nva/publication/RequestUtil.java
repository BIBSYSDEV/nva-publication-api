package no.unit.nva.publication;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtil {
    
    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
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
            identifier = requestInfo.getPathParameters().get(PUBLICATION_IDENTIFIER);
            logger.info("Requesting publication metadata for ID: {}", identifier);
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
    @SuppressWarnings("PMD.InvalidLogMessageFormat")
    public static String getOwner(RequestInfo requestInfo) throws ApiGatewayException {
        return attempt(requestInfo::getUserName).orElseThrow(fail -> new UnauthorizedException());
    }

    public static UserInstance createExternalUserInstance(RequestInfo requestInfo,
                                                          IdentityServiceClient identityServiceClient)
        throws UnauthorizedException {
        var client = attempt(() -> requestInfo.getClientId().orElseThrow())
                         .map(identityServiceClient::getExternalClient)
                         .orElseThrow(fail -> new UnauthorizedException());

        var resourceOwner = new ResourceOwner(
            new Username(client.getActingUser()),
            client.getCristinUrgUri()
        );

        return UserInstance.createExternalUser(resourceOwner, client.getCustomerUri());
    }

    public static UserInstance createInternalUserInstance(RequestInfo requestInfo) throws ApiGatewayException {
        String owner = RequestUtil.getOwner(requestInfo);
        URI customerId = requestInfo.getCurrentCustomer();
        return UserInstance.create(owner, customerId);
    }
}
