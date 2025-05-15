package no.unit.nva.publication;

import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactDto;
import no.unit.nva.model.associatedartifacts.file.FileDto;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissions;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.RequestInfo;

public final class PublicationResponseFactory {

    private PublicationResponseFactory() {
    }

    public static PublicationResponse create(Resource resource, RequestInfo requestInfo,
                                             IdentityServiceClient identityServiceClient) {
        var userInstance = getUserInstance(requestInfo, identityServiceClient);
        var publicationPermissions = PublicationPermissions.create(resource, userInstance);

        if (hasAuthenticatedAccessOnPublication(publicationPermissions)) {
            return createAuthenticatedResponse(publicationPermissions, resource, userInstance);
        } else {
            return createPublicResponse(publicationPermissions, resource, userInstance);
        }
    }

    private static boolean hasAuthenticatedAccessOnPublication(PublicationPermissions userStrategy) {
        return userStrategy.allowsAction(PublicationOperation.PARTIAL_UPDATE);
    }

    private static PublicationResponse createPublicResponse(PublicationPermissions publicationPermissions,
                                                            Resource resource, UserInstance userInstance) {
        var publicationResponse = PublicationMapper.convertValue(resource.toPublication(), PublicationResponse.class);
        publicationResponse.setAllowedOperations(emptySet());
        publicationResponse.setAssociatedArtifacts(extractFilteredAssociatedArtifactsList(publicationPermissions,
                                                                                          resource, userInstance));
        return publicationResponse;
    }

    private static PublicationResponseElevatedUser createAuthenticatedResponse(PublicationPermissions strategy,
                                                                               Resource resource,
                                                                               UserInstance userInstance) {
        var publicationResponse = PublicationMapper.convertValue(resource.toPublication(), PublicationResponseElevatedUser.class);
        publicationResponse.setAllowedOperations(strategy.getAllAllowedActions());

        publicationResponse.setAssociatedArtifacts(extractFilteredAssociatedArtifactsList(strategy, resource,
                                                                                          userInstance));
        return publicationResponse;
    }

    private static List<AssociatedArtifactDto> extractFilteredAssociatedArtifactsList(
        PublicationPermissions publicationPermissions,
        Resource resource, UserInstance userInstance) {

        return resource.getAssociatedArtifacts()
                   .stream()
                   .filter(
                       associatedArtifact -> isVisibleArtifact(publicationPermissions, associatedArtifact))
                   .map(AssociatedArtifact::toDto)
                   .map(artifact -> getFileDtoWithArtifactOperations(artifact, userInstance, resource))
                   .toList();
    }

    private static AssociatedArtifactDto getFileDtoWithArtifactOperations(
        AssociatedArtifactDto artifact, UserInstance userInstance, Resource resource) {
        if (artifact instanceof FileDto fileDto) {
            var file = resource.getFileEntry(fileDto.identifier()).orElseThrow();
            var filePermissions = FilePermissions.create(file, userInstance, resource);
            return fileDto.copy().withAllowedOperations(filePermissions.getAllAllowedActions()).build();
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
