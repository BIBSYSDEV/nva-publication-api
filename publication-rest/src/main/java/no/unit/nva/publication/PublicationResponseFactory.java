package no.unit.nva.publication;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import nva.commons.apigateway.RequestInfo;

public final class PublicationResponseFactory {

    private PublicationResponseFactory() {
    }

    public static PublicationResponse create(Publication publication, RequestInfo requestInfo,
                                             IdentityServiceClient identityServiceClient) {
        var userStrategy = getPublicationPermissionStrategy(requestInfo, publication, identityServiceClient);

        if (userStrategy.isPresent() && hasAuthenticatedAccessOnPublication(userStrategy.get())) {
            return createAuthenticatedResponse(userStrategy.get(), publication);
        } else {
            return createPublicResponse(publication);
        }
    }

    private static boolean hasAuthenticatedAccessOnPublication(PublicationPermissionStrategy userStrategy) {
        return userStrategy.allowsAction(PublicationOperation.UPDATE);
    }

    private static PublicationResponse createPublicResponse(Publication publication) {
        var publicationResponse = PublicationMapper.convertValue(publication, PublicationResponse.class);
        publicationResponse.setAllowedOperations(Set.of());
        return publicationResponse;
    }

    private static PublicationResponseElevatedUser createAuthenticatedResponse(PublicationPermissionStrategy strategy,
                                                                               Publication publication) {
        var publicationResponse = PublicationMapper.convertValue(publication, PublicationResponseElevatedUser.class);
        publicationResponse.setAllowedOperations(strategy.getAllAllowedActions());

        var filteredArtifacts = new AssociatedArtifactList(publication.getAssociatedArtifacts()
                                                               .stream()
                                                               .filter(
                                                                   associatedArtifact -> hasAccessToArtifact(strategy,
                                                                                                             associatedArtifact))
                                                               .toList());
        publicationResponse.setAssociatedArtifacts(filteredArtifacts);
        return publicationResponse;
    }

    private static boolean hasAccessToArtifact(PublicationPermissionStrategy strategy,
                                               AssociatedArtifact associatedArtifact) {
        return !(associatedArtifact instanceof HiddenFile)
               || strategy.allowsAction(
            PublicationOperation.READ_HIDDEN_FILES);
    }

    private static Optional<PublicationPermissionStrategy> getPublicationPermissionStrategy(RequestInfo requestInfo,
                                                                                            Publication publication,
                                                                                            IdentityServiceClient identityServiceClient) {
        return attempt(() -> RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient)).toOptional()
                   .map(userInstance -> PublicationPermissionStrategy.create(publication, userInstance));
    }
}
