package no.unit.nva.publication.model.business.logentry;

import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_CREATED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.User;
import org.junit.jupiter.api.Test;

class LogEntryTest {

    @Test
    void shouldDoRoundTripWithoutLossOfData() throws JsonProcessingException {
        var logEntry = new LogEntry(PUBLICATION_CREATED, Instant.now(), new User(randomString()));
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(logEntry);

        var roundTrippedLogEntry = JsonUtils.dtoObjectMapper.readValue(json, LogEntry.class);

        assertEquals(logEntry, roundTrippedLogEntry);
    }

    @Test
    void shouldParseLogTopicFromString() {
        assertDoesNotThrow(() -> LogTopic.fromValue(PUBLICATION_CREATED.getValue()));
    }
}