package no.unit.nva.cristin.lambda;

import java.util.ArrayList;
import no.unit.nva.cristin.mapper.AssociatedLinkExtractor;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;

public final class PublicationUpdater {

    private PublicationUpdater() {
    }

    public static PublicationRepresentations update(PublicationRepresentations publicationRepresentations) {
        var existingPublication = publicationRepresentations.getExistingPublication();
        existingPublication.setAssociatedArtifacts(updatedAssociatedLinks(publicationRepresentations));
        return publicationRepresentations;
    }

    private static AssociatedArtifactList updatedAssociatedLinks(
        PublicationRepresentations publicationRepresentations) {
        var incomingAssociatedLinks = publicationRepresentations.getIncomingPublication().getAssociatedArtifacts();
        var existingAssociatedLinks = publicationRepresentations.getExistingPublication().getAssociatedArtifacts();
        var list = new ArrayList<>(existingAssociatedLinks);
        list.addAll(incomingAssociatedLinks);
        return new AssociatedArtifactList(list.stream().distinct().toList());
    }
}
