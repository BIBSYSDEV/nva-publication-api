package no.unit.nva.publication.model.business;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Anthology;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record ResourceRelationship(SortableIdentifier parentIdentifier, SortableIdentifier childIdentifier) {

    private static final String PUBLICATION_PATH = "publication";

    public static Optional<ResourceRelationship> fromResource(Resource resource) {
        return getAnthologyPublicationIdentifierForPublishedResource(resource)
                   .map(parentIdentifier -> new ResourceRelationship(parentIdentifier, resource.getIdentifier()));
    }

    public static Optional<SortableIdentifier> getAnthologyPublicationIdentifierForPublishedResource(
        Resource resource) {
        return Optional.ofNullable(resource)
                   .filter(ResourceRelationship::isPublished)
                   .map(Resource::getEntityDescription)
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .filter(Anthology.class::isInstance)
                   .map(Anthology.class::cast)
                   .map(Anthology::getId)
                   .filter(ResourceRelationship::isPublicationId)
                   .map(SortableIdentifier::fromUri);
    }

    private static boolean isPublished(Resource resource) {
        return PUBLISHED.equals(resource.getStatus());
    }

    private static boolean isPublicationId(URI uri) {
        var uriWrapper = UriWrapper.fromUri(uri);
        return isSortableIdentifier(uriWrapper) && isPublicationPath(uriWrapper);
    }

    private static boolean isPublicationPath(UriWrapper uriWrapper) {
        return PUBLICATION_PATH.equals(uriWrapper.getPath().getPathElementByIndexFromEnd(1));
    }

    private static boolean isSortableIdentifier(UriWrapper uriWrapper) {
        return attempt(uriWrapper::getLastPathElement).map(SortableIdentifier::new).isSuccess();
    }
}
