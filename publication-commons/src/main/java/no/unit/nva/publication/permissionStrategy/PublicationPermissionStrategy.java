package no.unit.nva.publication.permissionStrategy;

import java.util.Set;
import no.unit.nva.model.Publication;
import nva.commons.apigateway.RequestInfo;

public class PublicationPermissionStrategy {
    private final Set<PermissionStrategy> permissionStrategies;

    public PublicationPermissionStrategy() {
        this.permissionStrategies = Set.of(
            new EditorPermissionStrategy(),
            new CuratorPermissionStrategy(),
            new ContributorPermissionStrategy(),
            new ResourceOwnerPermissionStrategy()
        );
    }

    public boolean hasPermissionToUnpublish(RequestInfo requestInfo, Publication publication) {
        for (PermissionStrategy strategy : permissionStrategies) {
            if (strategy.hasPermission(requestInfo, publication)) {
                return true;
            }
        }
        return false;
    }
}

