package no.unit.nva.publication;

import static java.util.Collections.emptySet;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Optional;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.RequestInfo;

public final class PublicationResponseFactory {

    private PublicationResponseFactory() {
    }

    public static PublicationResponse create(Publication publication, RequestInfo requestInfo,
                                             IdentityServiceClient identityServiceClient) {
        var userStrategy = getPublicationPermissionStrategy(requestInfo, publication, identityServiceClient);
        return userStrategy.filter(PublicationResponseFactory::hasAuthenticatedAccessOnPublication)
                   .map(strategy -> (PublicationResponse) createAuthenticatedResponse(strategy, publication))
                   .orElse(createPublicResponse(publication));
    }

    private static boolean hasAuthenticatedAccessOnPublication(PublicationPermissions userStrategy) {
        return userStrategy.allowsAction(PublicationOperation.UPDATE);
    }

    private static PublicationResponse createPublicResponse(Publication publication) {
        var publicationResponse = PublicationMapper.convertValue(publication, PublicationResponse.class);
        publicationResponse.setAllowedOperations(emptySet());
        return publicationResponse;
    }

    private static PublicationResponseElevatedUser createAuthenticatedResponse(PublicationPermissions strategy,
                                                                               Publication publication) {
        var publicationResponse = PublicationMapper.convertValue(publication, PublicationResponseElevatedUser.class);
        publicationResponse.setAllowedOperations(strategy.getAllAllowedActions());

        publicationResponse.setAssociatedArtifacts(extractFilteredAssociatedArtifactsList(strategy, publication));
        return publicationResponse;
    }

    private static AssociatedArtifactList extractFilteredAssociatedArtifactsList(PublicationPermissions strategy,
                                                                                 Publication publication) {
        return new AssociatedArtifactList(publication.getAssociatedArtifacts()
                                              .stream()
                                              .filter(
                                                  associatedArtifact -> hasAccessToArtifact(strategy,
                                                                                            associatedArtifact))
                                              .toList());
    }

    private static boolean hasAccessToArtifact(PublicationPermissions strategy,
                                               AssociatedArtifact associatedArtifact) {
        return !(associatedArtifact instanceof HiddenFile)
               || strategy.allowsAction(
            PublicationOperation.READ_HIDDEN_FILES);
    }

    private static Optional<PublicationPermissions> getPublicationPermissionStrategy(RequestInfo requestInfo,
                                                                                     Publication publication,
                                                                                     IdentityServiceClient identityServiceClient) {
        return attempt(() -> RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient)).toOptional()
                   .map(userInstance -> PublicationPermissions.create(publication, userInstance));
    }
}
