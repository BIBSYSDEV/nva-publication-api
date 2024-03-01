package no.unit.nva.publication;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtil {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String IMPORT_CANDIDATE_IDENTIFIER = "importCandidateIdentifier";
    public static final String TICKET_IDENTIFIER = "ticketIdentifier";
    public static final String FILE_IDENTIFIER = "fileIdentifier";
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

    public static SortableIdentifier getImportCandidateIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        return Optional.ofNullable(requestInfo.getPathParameters())
                   .map(params -> params.get(IMPORT_CANDIDATE_IDENTIFIER))
                   .map(SortableIdentifier::new)
                   .orElseThrow(() -> new BadRequestException("Could not get import candidate identifier!"));
    }

    public static UUID getFileIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        return Optional.ofNullable(requestInfo.getPathParameters())
                   .map(params -> params.get(FILE_IDENTIFIER))
                   .map(UUID::fromString)
                   .orElseThrow(() -> new BadRequestException("Could not get file identifier!"));
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

    private static UserInstance createExternalUserInstance(RequestInfo requestInfo,
                                                           IdentityServiceClient identityServiceClient)
        throws UnauthorizedException {
        var client =
            attempt(() -> identityServiceClient.getExternalClientByToken(requestInfo.getAuthHeader())).orElseThrow(
                fail -> new UnauthorizedException());

        Stream.of(client.getActingUser(), client.getCristinUrgUri(), client.getCustomerUri())
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(UnauthorizedException::new);

        var resourceOwner = new ResourceOwner(
            new Username(client.getActingUser()),
            client.getCristinUrgUri()
        );

        return UserInstance.createExternalUser(resourceOwner, client.getCustomerUri());
    }

    private static UserInstance createInternalUserInstance(RequestInfo requestInfo) throws ApiGatewayException {
        String owner = RequestUtil.getOwner(requestInfo);
        var customerId = requestInfo.getCurrentCustomer();
        var personCristinId = attempt(requestInfo::getPersonCristinId).toOptional().orElse(null);
        var topLevelOrg = attempt(requestInfo::getTopLevelOrgCristinId).map(Optional::get).toOptional().orElse(null);
        var accessRights = requestInfo.getAccessRights();
        return new UserInstance(owner, customerId, topLevelOrg, personCristinId, accessRights);
    }

    public static UserInstance createUserInstanceFromRequest(RequestInfo requestInfo,
                                                             IdentityServiceClient identityServiceClient)
        throws UnauthorizedException {

        try {
            return createInternalUserInstance(requestInfo);
        } catch (Exception e) {
            return createExternalUserInstance(requestInfo, identityServiceClient);
        }
    }
}
