package no.unit.nva.cristin.lambda;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.event.ConferenceLecture;
import no.unit.nva.model.time.Instant;
import no.unit.nva.model.time.Time;
import org.junit.jupiter.api.Test;

class PublicationUpdaterTest {

    @Test
    void shouldReturnUpdatedEventWithNullValuesWhenAllEventValuesAreMissing() {
        var existingPublication = randomPublication(ConferenceLecture.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyEvent());
        var incomingPublication = randomPublication(ConferenceLecture.class);
        incomingPublication.getEntityDescription().getReference().setPublicationContext(emptyEvent());
        var publicationRepresentations = getPublicationRepresentations(incomingPublication, existingPublication);
        var publication = PublicationUpdater.update(publicationRepresentations).getExistingPublication();

        assertEquals(publication.getEntityDescription().getReference().getPublicationContext(), emptyEvent());
    }

    @Test
    void shouldUpdatedEventWithTimeWhenExistingEventIsMissingTimeButNewEventHasTimeMissing() {
        var existingPublication = randomPublication(ConferenceLecture.class);
        existingPublication.getEntityDescription().getReference().setPublicationContext(emptyEvent());
        var incomingPublication = randomPublication(ConferenceLecture.class);
        var eventWithTime = eventWithTime();
        incomingPublication.getEntityDescription().getReference().setPublicationContext(eventWithTime);
        var publicationRepresentations = getPublicationRepresentations(incomingPublication, existingPublication);
        var publication = PublicationUpdater.update(publicationRepresentations).getExistingPublication();

        var event = (Event) publication.getEntityDescription().getReference().getPublicationContext();

        assertEquals(event.getTime(), eventWithTime.getTime());
    }

    private static Event eventWithTime() {
        return new Event(null, null,
                         new Instant(
                             java.time.Instant.now()),
                         null,
                         null, null);
    }

    @Test
    void shouldUpdateExistingContributorIdWhenMissingAndIncomingPublicationHasContributorWithId() {
        var contributorName = randomString();
        var existingPublication = randomPublication(DegreePhd.class);
        var contributorWithoutId = new Contributor.Builder().withIdentity(
            new Identity.Builder().withName(contributorName).build()).build();
        existingPublication.getEntityDescription().setContributors(List.of(contributorWithoutId));

        var incomingPublication = randomPublication(DegreePhd.class);
        var contributorId = randomUri();
        var contributorWithId = new Contributor.Builder().withIdentity(
            new Identity.Builder().withName(contributorName).withId(contributorId).build()).build();
        incomingPublication.getEntityDescription().setContributors(List.of(contributorWithId));

        var publicationRepresentations = getPublicationRepresentations(incomingPublication, existingPublication);
        var publication = PublicationUpdater.update(publicationRepresentations).getExistingPublication();

        assertEquals(publication.getEntityDescription().getContributors().getFirst().getIdentity().getId(),
                     contributorId);
    }

    @Test
    void shouldNotUpdateExistingContributorIdWhenMissingAndIncomingContributorDoesNotHaveTheSameName() {
        var existingPublication = randomPublication(DegreePhd.class);
        var contributorWithoutId = new Contributor.Builder().withIdentity(
            new Identity.Builder().withName(randomString()).build()).build();
        existingPublication.getEntityDescription().setContributors(List.of(contributorWithoutId));

        var incomingPublication = randomPublication(DegreePhd.class);
        var contributorId = randomUri();
        var contributorWithId = new Contributor.Builder().withIdentity(
            new Identity.Builder().withName(randomString()).withId(contributorId).build()).build();
        incomingPublication.getEntityDescription().setContributors(List.of(contributorWithId));

        var publicationRepresentations = getPublicationRepresentations(incomingPublication, existingPublication);
        var publication = PublicationUpdater.update(publicationRepresentations).getExistingPublication();

        assertNull(publication.getEntityDescription().getContributors().getFirst().getIdentity().getId());
    }

    private static PublicationRepresentations getPublicationRepresentations(Publication incomingPublication,
                                                                            Publication existingPublication) {
        return new PublicationRepresentations(null, incomingPublication, null).withExistingPublication(
            existingPublication);
    }

    private static Event emptyEvent() {
        return new Event(null, null, null, null, null, null);
    }
}