package no.unit.nva.cristin.lambda;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.instancetypes.event.ConferenceLecture;
import org.junit.jupiter.api.Test;

class PublicationUpdaterTest {

    @Test
    void shouldReturnUpdatedEventWithNullValuesWhenAllEventValuesAreMissing() {
        var existingPublication = randomPublication(ConferenceLecture.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyEvent());
        var incomingPublication = randomPublication(ConferenceLecture.class);
        incomingPublication.getEntityDescription().getReference().setPublicationContext(emptyEvent());
        var publicationRepresentations =
            new PublicationRepresentations(null, incomingPublication, null).withExistingPublication(existingPublication);
        var publication = PublicationUpdater.update(publicationRepresentations).getExistingPublication();

        assertEquals(publication.getEntityDescription().getReference().getPublicationContext(), emptyEvent());
    }

    private static Event emptyEvent() {
        return new Event(null, null, null,
                         null, null, null);
    }
}