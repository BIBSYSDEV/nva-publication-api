package no.unit.nva.publication.permission.strategy;

import java.util.Set;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public final class PublicationPermissionStrategy {
    private final Set<PermissionStrategy> permissionStrategies;
    private final RequestInfo requestInfo;

    private PublicationPermissionStrategy(RequestInfo requestInfo) {
        this.requestInfo = requestInfo;
        this.permissionStrategies = Set.of(
            new EditorPermissionStrategy(requestInfo),
            new CuratorPermissionStrategy(),
            new ContributorPermissionStrategy(),
            new ResourceOwnerPermissionStrategy()
        );
    }

    public static PublicationPermissionStrategy fromRequestInfo(RequestInfo requestInfo) {
        return new PublicationPermissionStrategy(requestInfo);
    }

    public boolean hasPermissionToUnpublish(Publication publication) {
        return permissionStrategies.stream()
                   .anyMatch(permissionStrategy -> permissionStrategy.hasPermission(requestInfo, publication));
    }
}

