package no.unit.nva.publication.model.utils;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.core.paths.UriWrapper;

public final class PublicationUtil {

    private static final String PUBLICATION_PATH = "publication";

    private PublicationUtil() {
    }

    public static Optional<SortableIdentifier> getAnthologyPublicationIdentifier(Resource resource) {
        return Optional.ofNullable(resource)
                   .map(Resource::getEntityDescription)
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .filter(Anthology.class::isInstance)
                   .map(Anthology.class::cast)
                   .map(Anthology::getId)
                   .filter(PublicationUtil::isPublicationId)
                   .map(SortableIdentifier::fromUri);
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
