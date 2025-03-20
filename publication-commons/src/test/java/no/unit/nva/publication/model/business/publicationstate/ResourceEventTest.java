package no.unit.nva.publication.model.business.publicationstate;

import static no.unit.nva.publication.model.business.logentry.LogTopic.DOI_ASSIGNED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.DOI_REJECTED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.DOI_REQUESTED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.DOI_RESERVED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_CREATED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_DELETED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_IMPORTED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_MERGED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_PUBLISHED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_REPUBLISHED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceEventTest {

    public static Stream<Arguments> resourceEventProvider() {
        return Stream.of(Arguments.of(new CreatedResourceEvent(Instant.now(), randomUser(), randomUri(), identifier()),
                                      PUBLICATION_CREATED), Arguments.of(
                             new PublishedResourceEvent(Instant.now(), randomUser(), randomUri(), identifier()),
                             PUBLICATION_PUBLISHED),
                         Arguments.of(
                             new UnpublishedResourceEvent(Instant.now(), randomUser(), randomUri(), identifier()),
                             PUBLICATION_UNPUBLISHED),
                         Arguments.of(new DeletedResourceEvent(Instant.now(), randomUser(), randomUri(), identifier()),
                                      PUBLICATION_DELETED), Arguments.of(
                new RepublishedResourceEvent(Instant.now(), randomUser(), randomUri(), identifier()),
                PUBLICATION_REPUBLISHED), Arguments.of(
                ImportedResourceEvent.fromImportSource(getImportSource(), randomUserInstance(), Instant.now()),
                PUBLICATION_IMPORTED),
                         Arguments.of(DoiReservedEvent.create(randomUserInstance(), Instant.now()), DOI_RESERVED),
                         Arguments.of(MergedResourceEvent.fromImportSource(getImportSource(), randomUserInstance(),
                                                                           Instant.now()), PUBLICATION_MERGED));
    }

    public static Stream<Arguments> ticketEventProvider() {
        return Stream.of(Arguments.of(DoiRequestedEvent.create(randomUserInstance(), Instant.now()), DOI_REQUESTED),
                         Arguments.of(DoiAssignedEvent.create(randomUserInstance(), Instant.now()), DOI_ASSIGNED),
                         Arguments.of(DoiRejectedEvent.create(randomUserInstance(), Instant.now()), DOI_REJECTED));
    }

    @ParameterizedTest
    @MethodSource("resourceEventProvider")
    void shouldConvertResourceEventToLogEntryWithExpectedTopic(ResourceEvent resourceEvent, LogTopic expectedLogTopic) {
        var logEntry = resourceEvent.toLogEntry(identifier(), null);

        assertEquals(expectedLogTopic, logEntry.topic());
    }

    @ParameterizedTest
    @MethodSource("ticketEventProvider")
    void shouldConvertTicketEventToLogEntryWithExpectedTopic(TicketEvent resourceEvent, LogTopic expectedLogTopic) {
        var logEntry = resourceEvent.toLogEntry(identifier(), identifier(), null);

        assertEquals(expectedLogTopic, logEntry.topic());
    }

    private static ImportSource getImportSource() {
        return new ImportSource(Source.BRAGE, "A");
    }

    private static UserInstance randomUserInstance() {
        return UserInstance.create(randomString(), randomUri());
    }

    private static SortableIdentifier identifier() {
        return SortableIdentifier.next();
    }

    private static User randomUser() {
        return new User(randomString());
    }
}