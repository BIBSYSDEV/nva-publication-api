package no.unit.nva.publication.model.business.publicationstate;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceEventTest {

    public static Stream<Arguments> resourceEventProvider() {
        return Stream.of(Arguments.of(new CreatedResourceEvent(Instant.now(), new User(randomString()), randomUri()),
                                      LogTopic.PUBLICATION_CREATED),
                         Arguments.of(new PublishedResourceEvent(Instant.now(), new User(randomString()), randomUri()),
                                      LogTopic.PUBLICATION_PUBLISHED), Arguments.of(
                new UnpublishedResourceEvent(Instant.now(), new User(randomString()), randomUri()),
                LogTopic.PUBLICATION_UNPUBLISHED),
                         Arguments.of(new DeletedResourceEvent(Instant.now(), new User(randomString()), randomUri()),
                                      LogTopic.PUBLICATION_DELETED),
                         Arguments.of(new RepublishedResourceEvent(Instant.now(), new User(randomString()), randomUri()),
                                      LogTopic.PUBLICATION_REPUBLISHED));
    }

    @ParameterizedTest
    @MethodSource("resourceEventProvider")
    void shouldConvertResourceEventToLogEntryWithExpectedTopic(ResourceEvent resourceEvent, LogTopic expectedLogTopic) {
        var logEntry = resourceEvent.toLogEntry(SortableIdentifier.next(), null);

        assertEquals(expectedLogTopic, logEntry.topic());
    }
}