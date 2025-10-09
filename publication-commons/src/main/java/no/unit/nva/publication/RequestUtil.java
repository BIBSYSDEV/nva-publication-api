package no.unit.nva.publication;

import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.HttpHeaders;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.ThirdPartySystem;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtil {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String IMPORT_CANDIDATE_IDENTIFIER = "importCandidateIdentifier";
    public static final String TICKET_IDENTIFIER = "ticketIdentifier";
    public static final String FILE_IDENTIFIER = "fileIdentifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    private static final Logger logger = LoggerFactory.getLogger(RequestUtil.class);
    public static final String COULD_NOT_GET_FILE_IDENTIFIER = "Could not get file identifier!";

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
                   .orElseThrow(() -> new BadRequestException(COULD_NOT_GET_FILE_IDENTIFIER));
    }

    public static SortableIdentifier getFileEntryIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        return Optional.ofNullable(requestInfo.getPathParameters())
                   .map(params -> params.get(FILE_IDENTIFIER))
                   .map(SortableIdentifier::new)
                   .orElseThrow(() -> new BadRequestException(COULD_NOT_GET_FILE_IDENTIFIER));
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

    public static Optional<String> getETagFromIfMatchHeader(RequestInfo requestInfo) {
        return requestInfo.getHeaderOptional(HttpHeaders.IF_MATCH);
    }

    public static Optional<String> getETagFromIfNoneMatchHeader(RequestInfo requestInfo) {
        return requestInfo.getHeaderOptional(HttpHeaders.IF_NONE_MATCH);
    }

    private static UserInstance createClientCredentialUserInstance(RequestInfo requestInfo,
                                                                   IdentityServiceClient identityServiceClient)
        throws UnauthorizedException {
        var client = attempt(() -> requestInfo.getClientId().orElseThrow())
                         .map(identityServiceClient::getExternalClient)
                         .orElseThrow(fail -> new UnauthorizedException());

        var resourceOwner = new ResourceOwner(
            new Username(client.getActingUser()),
            client.getCristinUrgUri()
        );

        var thirdPartySystem = requestInfo.getHeaderOptional("System").map(
            ThirdPartySystem::fromValue).orElse(null);


        return requestInfo.clientIsInternalBackend()
                   ? UserInstance.createBackendUser(resourceOwner, client.getCustomerUri())
                   : UserInstance.createExternalUser(resourceOwner, client.getCustomerUri(), thirdPartySystem);
    }

    private static UserInstance createDataportenUserInstance(RequestInfo requestInfo) throws ApiGatewayException {
        String owner = RequestUtil.getOwner(requestInfo);
        var customerId = requestInfo.getCurrentCustomer();
        var personCristinId = attempt(requestInfo::getPersonCristinId).toOptional().orElse(null);
        var topLevelOrg = attempt(requestInfo::getTopLevelOrgCristinId).map(Optional::get).toOptional().orElse(null);
        var personAffiliation = attempt(requestInfo::getPersonAffiliation).orElse(failure -> null);
        var accessRights = requestInfo.getAccessRights();
        return new UserInstance(owner, customerId, topLevelOrg, personAffiliation, personCristinId, accessRights,
                                UserClientType.INTERNAL, null);
    }

    public static UserInstance createUserInstanceFromRequest(RequestInfo requestInfo,
                                                             IdentityServiceClient identityServiceClient)
        throws UnauthorizedException {
        try {
            return requestInfo.clientIsThirdParty() || requestInfo.clientIsInternalBackend()
                       ? createClientCredentialUserInstance(requestInfo, identityServiceClient)
                       : createDataportenUserInstance(requestInfo);
        } catch (ApiGatewayException e) {
            e.printStackTrace();
            throw new UnauthorizedException(e.getMessage());
        }
    }
}
