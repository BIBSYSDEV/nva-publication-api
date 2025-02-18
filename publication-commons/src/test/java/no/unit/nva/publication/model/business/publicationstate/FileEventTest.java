package no.unit.nva.publication.model.business.publicationstate;

import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FileEventTest {

    public static Stream<Arguments> stateProvider() {
        return Stream.of(Arguments.of(new FileUploadedEvent(Instant.now(), randomUser(), SortableIdentifier.next())),
                         Arguments.of(
                             new FileApprovedEvent(Instant.now(), randomUser(), SortableIdentifier.next())),
                         Arguments.of(new FileRejectedEvent(Instant.now(), randomUser(), SortableIdentifier.next())),
                         Arguments.of(new FileDeletedEvent(Instant.now(), randomUser(), SortableIdentifier.next())),
                         Arguments.of(new FileImportedEvent(Instant.now(), randomUser(), SortableIdentifier.next(),
                                                            ImportSource.fromSource(Source.SCOPUS))),
                         Arguments.of(new FileRetractedEvent(Instant.now(), randomUser(), SortableIdentifier.next())),
                         Arguments.of(new FileHiddenEvent(Instant.now(), randomUser(), SortableIdentifier.next())));
    }

    @ParameterizedTest
    @MethodSource("stateProvider")
    void shouldDoRoundTripWithoutLossOfData(FileEvent state) throws JsonProcessingException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(state);
        var roundTrippedState = JsonUtils.dtoObjectMapper.readValue(json, FileEvent.class);
        var fileEntry = FileEntry.create(randomOpenFile(), SortableIdentifier.next(),
                                        UserInstance.create(randomUser(), randomUri()));

        assertEquals(state, roundTrippedState);
        assertDoesNotThrow(() -> roundTrippedState.toLogEntry(fileEntry,
                                                              LogUser.fromResourceEvent(randomUser(), randomUri())));
    }

    private static User randomUser() {
        return new User(randomString());
    }
}