package no.unit.nva.publication;

import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactResponse;
import no.unit.nva.model.associatedartifacts.file.FileResponse;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissions;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.RequestInfo;

public final class PublicationResponseFactory {

    private PublicationResponseFactory() {
    }

    public static PublicationResponse create(Publication publication, RequestInfo requestInfo,
                                             IdentityServiceClient identityServiceClient) {
        var userInstance = getUserInstance(requestInfo, identityServiceClient);
        var publicationPermissions = PublicationPermissions.create(publication, userInstance);

        if(hasAuthenticatedAccessOnPublication(publicationPermissions)) {
            return createAuthenticatedResponse(publicationPermissions, publication, userInstance);
        } else {
            return createPublicResponse(publicationPermissions, publication, userInstance);
        }
    }

    private static boolean hasAuthenticatedAccessOnPublication(PublicationPermissions userStrategy) {
        return userStrategy.allowsAction(PublicationOperation.UPDATE);
    }

    private static PublicationResponse createPublicResponse(PublicationPermissions publicationPermissions,
                                                            Publication publication, UserInstance userInstance) {
        var publicationResponse = PublicationMapper.convertValue(publication, PublicationResponse.class);
        publicationResponse.setAllowedOperations(emptySet());
        publicationResponse.setAssociatedArtifacts(extractFilteredAssociatedArtifactsList(publicationPermissions,
                                                                                          publication, userInstance));
        return publicationResponse;
    }

    private static PublicationResponseElevatedUser createAuthenticatedResponse(PublicationPermissions strategy,
                                                                               Publication publication, UserInstance userInstance) {
        var publicationResponse = PublicationMapper.convertValue(publication, PublicationResponseElevatedUser.class);
        publicationResponse.setAllowedOperations(strategy.getAllAllowedActions());

        publicationResponse.setAssociatedArtifacts(extractFilteredAssociatedArtifactsList(strategy, publication,
                                                                                          userInstance));
        return publicationResponse;
    }

    private static List<AssociatedArtifactResponse> extractFilteredAssociatedArtifactsList(
        PublicationPermissions publicationPermissions,
        Publication publication, UserInstance userInstance) {

        return publication.getAssociatedArtifacts()
                   .stream()
                   .filter(
                       associatedArtifact -> isVisibleArtifact(publicationPermissions, associatedArtifact))
                   .map(artifact -> dtoObjectMapper.convertValue(artifact, AssociatedArtifactResponse.class))
                   .map(artifact -> applyArtifactOperations(artifact, userInstance, publication))
                   .toList();
    }

    private static AssociatedArtifactResponse applyArtifactOperations(
        AssociatedArtifactResponse artifact, UserInstance userInstance, Publication publication) {
        if (artifact instanceof FileResponse fileResponse) {
            var file = publication.getFile(fileResponse.identifier()).orElseThrow();
            var filePermissions = FilePermissions.create(file, userInstance, publication);
            return fileResponse.copy().withAllowedOperations(filePermissions.getAllAllowedActions()).build();
        }

        return artifact;
    }

    private static boolean isVisibleArtifact(PublicationPermissions strategy, AssociatedArtifact associatedArtifact) {
        return nonNull(strategy) ? hasAccessToArtifact(strategy,
                                                       associatedArtifact) :
                                                                               AssociatedArtifact.PUBLIC_ARTIFACT_TYPES.contains(
                                                                                   associatedArtifact.getClass());
    }

    private static boolean hasAccessToArtifact(PublicationPermissions strategy,
                                               AssociatedArtifact associatedArtifact) {
        return !(associatedArtifact instanceof HiddenFile)
               || strategy.allowsAction(
            PublicationOperation.READ_HIDDEN_FILES);
    }

    private static UserInstance getUserInstance(RequestInfo requestInfo,
                    IdentityServiceClient identityServiceClient) {
        return attempt(() -> RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient)).toOptional()
                   .orElse(null);
    }

}
