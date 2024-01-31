package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.publication.RequestUtil.createExternalUserInstance;
import static no.unit.nva.publication.RequestUtil.createInternalUserInstance;
import java.util.Set;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class PublicationPermissionStrategy {
    private final Set<PermissionStrategy> permissionStrategies;
    private final RequestInfo requestInfo;
    private final Publication publication;

    // todo: send inn publication og access rights (i stedet for requestInfo)
    private PublicationPermissionStrategy(Publication publication ,RequestInfo requestInfo, UserInstance userInstance) {
        this.publication = publication;
        this.requestInfo = requestInfo;
        this.permissionStrategies = Set.of(
            new EditorPermissionStrategy(),
            new CuratorPermissionStrategy(userInstance),
            new ContributorPermissionStrategy(),
            new ResourceOwnerPermissionStrategy(),
            new TrustedThirdPartyStrategy(userInstance)
        );
    }

    private PublicationPermissionStrategy(Publication publication){
        // No access provided while keeping the interface consistent
        this.publication = publication;
        this.requestInfo = null;
        this.permissionStrategies = Set.of();
    }

    public static PublicationPermissionStrategy fromRequestInfo(RequestInfo requestInfo,
                                                                IdentityServiceClient identityServiceClient) {
        try {
            //todo add publication
            return new PublicationPermissionStrategy(null, requestInfo, createUserInstanceFromRequest(requestInfo, identityServiceClient));
        } catch (UnauthorizedException e) {
            return noAccessPublicationPermissionStrategy(null);
        }
    }

    public boolean hasPermissionToDelete(Publication publication) {
        return permissionStrategies.stream()
                   .anyMatch(permissionStrategy -> permissionStrategy.hasPermissionToDelete(requestInfo, publication));
    }

    public boolean hasPermissionToUpdate(Publication publication) {
        return permissionStrategies.stream()
                   .anyMatch(permissionStrategy -> permissionStrategy.hasPermissionToUpdate(requestInfo, publication));
    }

    private static PublicationPermissionStrategy noAccessPublicationPermissionStrategy(Publication publication) {
        return new PublicationPermissionStrategy(publication);
    }

    public boolean hasPermissionToUnpublish(Publication publication) {
        return permissionStrategies.stream()
                   .anyMatch(permissionStrategy -> permissionStrategy.hasPermissionToUnpublish(requestInfo, publication));
    }

    private static UserInstance createUserInstanceFromRequest(RequestInfo requestInfo,
                                                              IdentityServiceClient identityServiceClient)
        throws UnauthorizedException {
        try {
            return requestInfo.clientIsThirdParty()
                       ? createExternalUserInstance(requestInfo, identityServiceClient)
                       : createInternalUserInstance(requestInfo);
        } catch (ApiGatewayException e) {
            throw new UnauthorizedException(e.getMessage());
        }
    }
}

