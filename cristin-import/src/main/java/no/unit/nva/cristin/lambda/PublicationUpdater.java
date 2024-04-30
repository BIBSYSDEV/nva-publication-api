package no.unit.nva.cristin.lambda;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.PublicationContext;

public final class PublicationUpdater {

    private PublicationUpdater() {
    }

    public static PublicationRepresentations update(PublicationRepresentations publicationRepresentations) {
        var existingPublication = publicationRepresentations.getExistingPublication();
        existingPublication.setAssociatedArtifacts(updatedAssociatedLinks(publicationRepresentations));
        existingPublication.setEntityDescription(updatedEntityDescription(publicationRepresentations));
        return publicationRepresentations;
    }

    private static EntityDescription updatedEntityDescription(PublicationRepresentations publicationRepresentations) {
        return publicationRepresentations.getExistingPublication().getEntityDescription().copy()
                   .withReference(updateReference(publicationRepresentations))
                   .withTags(updatedTags(publicationRepresentations))
                   .build();
    }

    private static List<String> updatedTags(PublicationRepresentations publicationRepresentations) {
        var existingTags = publicationRepresentations.getExistingPublication().getEntityDescription().getTags();
        var incomingTags = publicationRepresentations.getIncomingPublication().getEntityDescription().getTags();
        var list = new ArrayList<>(existingTags);
        list.addAll(incomingTags);
        return list.stream().distinct().toList();
    }

    private static Reference updateReference(PublicationRepresentations publicationRepresentations) {
        var existinReference = publicationRepresentations.getExistingPublication().getEntityDescription().getReference();
        existinReference.setPublicationContext(updatePublicationContext(publicationRepresentations));
        return existinReference;

    }

    private static PublicationContext updatePublicationContext(PublicationRepresentations publicationRepresentations) {
        var existingPublicationContext = getPublicationContext(publicationRepresentations.getExistingPublication());
        var incomingPublicationContext = getPublicationContext(publicationRepresentations.getIncomingPublication());
        if (existingPublicationContext instanceof Event existingEvent && incomingPublicationContext instanceof Event incomingEvent) {
            var existingPlace = existingEvent.getPlace();
            var incomingPlace = incomingEvent.getPlace();
            if (isNull(existingPlace) && nonNull(incomingPlace)) {
                return new Event.Builder()
                           .withLabel(existingEvent.getLabel())
                           .withAgent(existingEvent.getAgent())
                           .withTime(existingEvent.getTime())
                           .withProduct(existingEvent.getProduct().orElse(null))
                           .withSubEvent(existingEvent.getSubEvent().orElse(null))
                           .withPlace(incomingPlace)
                           .build();
            } else {
                return existingEvent;
            }
        } else {
            return existingPublicationContext;
        }
    }

    private static PublicationContext getPublicationContext(Publication publication) {
        return publication
                   .getEntityDescription()
                   .getReference()
                   .getPublicationContext();
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
