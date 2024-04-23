package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinMainCategory.MEDIA_CONTRIBUTION;
import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedLink;

public final class AssociatedLinkExtractor {

    private static final List<String> ASSOCIATED_URI_TYPES = List.of("DATA", "FULLTEKST", "OMTALE");
    private static final String HANDLE_DOMAIN = "handle";

    private AssociatedLinkExtractor() {
    }

    public static List<AssociatedArtifact> extractAssociatedLinks(CristinObject cristinObject) {
        if (cristinObject.getMainCategory().equals(MEDIA_CONTRIBUTION)) {
            return cristinObject.getCristinAssociatedUris().stream()
                       .filter(AssociatedLinkExtractor::isNotAHandle)
                       .filter(AssociatedLinkExtractor::isAssociatedLink)
                       .map(AssociatedLinkExtractor::toAssociatedLink)
                       .toList();
        }
        return List.of();
    }

    private static boolean isNotAHandle(CristinAssociatedUri cristinAssociatedUri) {
        return !cristinAssociatedUri.getUrl().contains(HANDLE_DOMAIN);
    }

    private static AssociatedArtifact toAssociatedLink(CristinAssociatedUri cristinAssociatedUri) {
        return new AssociatedLink(cristinAssociatedUri.toURI(), null, null);
    }

    private static boolean isAssociatedLink(CristinAssociatedUri cristinAssociatedUri) {
        return ASSOCIATED_URI_TYPES.contains(cristinAssociatedUri.getUrlType());
    }
}
