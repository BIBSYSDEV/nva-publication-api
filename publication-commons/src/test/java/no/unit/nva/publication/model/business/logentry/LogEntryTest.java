package no.unit.nva.publication.model.business.logentry;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_CREATED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class LogEntryTest extends ResourcesLocalTest {

    private ResourceService resourceService;

    @Override
    @BeforeEach
    public void init() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
    }

    @ParameterizedTest
    @EnumSource(value = LogTopic.class, mode = Mode.EXCLUDE)
    void shouldDoRoundTripWithoutLossOfData(LogTopic logTopic) throws JsonProcessingException {
        var logEntry = randomLogEntry(SortableIdentifier.next(), logTopic);
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(logEntry);

        var roundTrippedLogEntry = JsonUtils.dtoObjectMapper.readValue(json, LogEntry.class);

        assertEquals(logEntry, roundTrippedLogEntry);
    }

    @Test
    void shouldParseLogTopicFromString() {
        assertDoesNotThrow(() -> LogTopic.fromValue(PUBLICATION_CREATED.getValue()));
    }

    @Test
    void shouldPersistLogEntriesForResourceAndFetchAllLogEntriesForResource() throws BadRequestException {
        var publication = randomPublication();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));

        var firstLogEntry = randomLogEntry(persistedPublication.getIdentifier(), LogTopic.PUBLICATION_UNPUBLISHED);
        var secondLogEntry = randomLogEntry(persistedPublication.getIdentifier(), LogTopic.PUBLICATION_DELETED);

        firstLogEntry.persist(resourceService);
        secondLogEntry.persist(resourceService);

        var logEntries = Resource.fromPublication(persistedPublication).fetchLogEntries(resourceService);

        assertTrue(logEntries.containsAll(List.of(firstLogEntry, secondLogEntry)));
    }

    private static LogEntry randomLogEntry(SortableIdentifier identifier, LogTopic logTopic) {
        return new LogEntry(SortableIdentifier.next(), identifier, logTopic, Instant.now(),
                            new User(randomString()), randomUri());
    }
}