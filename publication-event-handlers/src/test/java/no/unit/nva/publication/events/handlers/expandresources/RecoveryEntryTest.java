package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.RESOURCE_UPDATE_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.expandresources.RecoveryEntry.FILE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.UserInstance;
import org.junit.jupiter.api.Test;

class RecoveryEntryTest {

    @Test
    void shouldCreateRecoveryEntryFromExpandedMessage() {
        var expandedDateEntry = ExpandedMessage.builder().build();
        var recoveryEntry = RecoveryEntry.fromExpandedDataEntry(expandedDateEntry);

        assertThat(recoveryEntry.getType(), is(equalTo("Message")));
    }

    @Test
    void shouldThrowExceptionWhenCreatingRecoveryEntryFromUnsupportedFromExpandedEntry() {
        var expandedDateEntry = new ExpandedImportCandidate();

        assertThrows(IllegalStateException.class, () -> RecoveryEntry.fromExpandedDataEntry(expandedDateEntry));
    }

    @Test
    void shouldThrowExceptionWhenCreatingRecoveryEntryFromUnsupportedEntity() {
        var entity = mock(Entity.class);
        var dataEntryUpdateEvent = new DataEntryUpdateEvent(RESOURCE_UPDATE_EVENT_TOPIC, entity, entity);

        assertThrows(IllegalStateException.class, () -> RecoveryEntry.fromDataEntryUpdateEvent(dataEntryUpdateEvent));
    }

    @Test
    void shouldCreateRecoveryEntryForFileEntryFromDataEntryUpdateEvent() {
        var fileEntry = FileEntry.create(randomOpenFile(), SortableIdentifier.next(),
                                         UserInstance.create(randomString(), randomUri()));

        var dataEntryUpdateEvent = new DataEntryUpdateEvent(randomString(), fileEntry, fileEntry);

        assertEquals(FILE, RecoveryEntry.fromDataEntryUpdateEvent(dataEntryUpdateEvent).getType());
    }
}