package no.unit.nva.publication.events.model;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublicationWithStatus;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.api.Test;

class PublicationEventReferenceTest {

    @Test
    void shouldThrowRuntimeExceptionWhenCreatingPublicationEventReferenceForNonPublication() {
        var event = new DataEntryUpdateEvent(randomString(), new Message(), new Message());

        assertThrows(RuntimeException.class, () -> PublicationEventReference.create(randomString(), null, event));
    }

    @Test
    void shouldProducePublicationEventReferenceWithProvidedTopic() {
        var topic = randomString();
        var reference = PublicationEventReference.create(topic, null, randomDataEntryEvent());

        assertEquals(topic, reference.getTopic());
    }

    @Test
    void shouldProducePublicationEventReferenceWithProvidedUri() {
        var uri = randomUri();
        var reference = PublicationEventReference.create(randomString(), uri, randomDataEntryEvent());

        assertEquals(uri, reference.getUri());
    }

    @Test
    void shouldProducePublicationEventReferenceWithPublicationOldAndNewStatus() {
        var oldImage = randomPublicationWithStatus(PublicationStatus.DELETED);
        var newImage = randomPublicationWithStatus(PublicationStatus.DRAFT);
        var event = new DataEntryUpdateEvent(randomString(), Resource.fromPublication(oldImage), Resource.fromPublication(newImage));
        var reference = PublicationEventReference.create(randomString(), randomUri(), event);

        assertEquals(oldImage.getStatus(), reference.getOldStatus());
        assertEquals(newImage.getStatus(), reference.getNewStatus());
    }

    @Test
    void shouldProducePublicationEventReferenceWithPublicationOldAndNewType() {
        var oldImage = Resource.fromPublication(randomPublication(JournalArticle.class));
        var newImage = Resource.fromPublication(randomPublication(AcademicChapter.class));
        var event = new DataEntryUpdateEvent(randomString(), oldImage, newImage);
        var reference = PublicationEventReference.create(randomString(), randomUri(), event);

        assertEquals(oldImage.getInstanceType().orElseThrow(), reference.getOldType());
        assertEquals(newImage.getInstanceType().orElseThrow(), reference.getNewType());
    }

    @Test
    void shouldProducePublicationEventReferenceWithPublicationIdentifierWhenOldImageIsMissing() {
        var newImage = Resource.fromPublication(randomPublication(AcademicChapter.class));
        var event = new DataEntryUpdateEvent(randomString(), null, newImage);
        var reference = PublicationEventReference.create(randomString(), randomUri(), event);

        assertEquals(newImage.getIdentifier(), reference.getIdentifier());
    }

    @Test
    void shouldProducePublicationEventReferenceWithPublicationIdentifierWhenNewImageIsMissing() {
        var oldImage = Resource.fromPublication(randomPublication(AcademicChapter.class));
        var event = new DataEntryUpdateEvent(randomString(), oldImage, null);
        var reference = PublicationEventReference.create(randomString(), randomUri(), event);

        assertEquals(oldImage.getIdentifier(), reference.getIdentifier());
    }

    private static DataEntryUpdateEvent randomDataEntryEvent() {
        return new DataEntryUpdateEvent(randomString(), Resource.fromPublication(randomPublication()),
                                        Resource.fromPublication(randomPublication()));
    }
}