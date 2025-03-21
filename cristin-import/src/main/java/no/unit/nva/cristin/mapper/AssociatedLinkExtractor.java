package no.unit.nva.cristin.mapper;

import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.RelationType;

public final class AssociatedLinkExtractor {

    public static final String DATA = "DATA";
    public static final String FULLTEKST = "FULLTEKST";
    public static final String OMTALE = "OMTALE";
    private static final List<String> ASSOCIATED_URI_TYPES = List.of(DATA, FULLTEKST, OMTALE);
    private static final String HANDLE_DOMAIN = "handle";

    private AssociatedLinkExtractor() {
    }

    public static List<AssociatedArtifact> extractAssociatedLinks(CristinObject cristinObject) {
        return cristinObject.getCristinAssociatedUris()
                   .stream()
                   .filter(AssociatedLinkExtractor::isNotAHandle)
                   .filter(AssociatedLinkExtractor::isAssociatedLink)
                   .filter(CristinAssociatedUri::isValidUri)
                   .map(AssociatedLinkExtractor::toAssociatedLink)
                   .toList();
    }

    private static boolean isNotAHandle(CristinAssociatedUri cristinAssociatedUri) {
        return !cristinAssociatedUri.getUrl().contains(HANDLE_DOMAIN);
    }

    private static AssociatedArtifact toAssociatedLink(CristinAssociatedUri cristinAssociatedUri) {
        return new AssociatedLink(cristinAssociatedUri.toURI(), null, null, getRelationType(cristinAssociatedUri));
    }

    private static RelationType getRelationType(CristinAssociatedUri cristinAssociatedUri) {
        return switch (cristinAssociatedUri.getUrlType()) {
            case DATA -> RelationType.DATASET;
            case FULLTEKST -> RelationType.SAME_AS;
            case OMTALE -> RelationType.MENTION;
            default -> null;
        };
    }

    private static boolean isAssociatedLink(CristinAssociatedUri cristinAssociatedUri) {
        return ASSOCIATED_URI_TYPES.contains(cristinAssociatedUri.getUrlType());
    }
}
