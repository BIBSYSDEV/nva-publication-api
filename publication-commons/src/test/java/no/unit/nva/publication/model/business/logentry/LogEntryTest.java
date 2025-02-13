package no.unit.nva.publication.model.business.logentry;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_CREATED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.unit.nva.clients.GetCustomerResponse;
import no.unit.nva.clients.GetUserResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationstate.DoiRequestedEvent;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class LogEntryTest extends ResourcesLocalTest {

    private ResourceService resourceService;
    private TicketService ticketService;

    @Override
    @BeforeEach
    public void init() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        ticketService = getTicketService();
    }

    @ParameterizedTest
    @EnumSource(value = LogTopic.class, mode = Mode.EXCLUDE)
    void shouldDoRoundTripWithoutLossOfData(LogTopic logTopic) throws JsonProcessingException {
        var logEntry = randomLogEntry(SortableIdentifier.next(), logTopic);
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(logEntry);

        var roundTrippedLogEntry = JsonUtils.dtoObjectMapper.readValue(json, PublicationLogEntry.class);

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

    @Test
    void shouldCreateLogUserFromGetUserResponse() {
        var getUserResponse = GetUserResponse.builder()
                                 .withUsername(randomString())
                                 .withGivenName(randomString())
                                 .withFamilyName(randomString())
                                 .withInstitution(randomUri())
                                 .build();
        var getCustomerResponse = new GetCustomerResponse(randomUri(), UUID.randomUUID(), randomString(), randomString(),
                                                      randomString(), randomUri());
        assertNotNull(LogUser.create(getUserResponse, getCustomerResponse));
    }

    @Test
    void shouldCreateLogUserFromUsername() {
        assertNotNull(LogUser.fromResourceEvent(new User(randomString()), randomUri()));
    }

    @Test
    void shouldCreateLogInstitutionFromGetCustomerResponse() {
        var getCustomerResponse = new GetCustomerResponse(randomUri(), UUID.randomUUID(), randomString(), randomString(),
                                                      randomString(), randomUri());

        assertNotNull(LogOrganization.fromGetCustomerResponse(getCustomerResponse));
    }

    @Test
    void shouldCreateLogInstitutionFromCristinId() {
        assertNotNull(LogOrganization.fromCristinId(randomUri()));
    }

    @Test
    void shouldPersistFileLogEntry() throws BadRequestException {
        var publication = randomPublication();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));
        var fileLogEntry = randomFileLogEntry(persistedPublication);

        fileLogEntry.persist(resourceService);

        var logEntries = Resource.fromPublication(persistedPublication).fetchLogEntries(resourceService);

        assertTrue(logEntries.contains(fileLogEntry));
    }

    @Test
    void shouldPersistTicketLogEntry() throws ApiGatewayException {
        var publication = randomPublication();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));
        var doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(persistedPublication))
                             .persistNewTicket(ticketService);
        var logEntry = DoiRequestedEvent.create(UserInstance.fromPublication(publication), Instant.now())
                                      .toLogEntry(persistedPublication.getIdentifier(), doiRequest.getIdentifier(),
                                                  randomLogUser());
        logEntry.persist(resourceService);

        var logEntries = Resource.fromPublication(persistedPublication).fetchLogEntries(resourceService);

        assertTrue(logEntries.contains(logEntry));
    }

    @Test
    void shouldSerializeLogUserWithUserName() throws JsonProcessingException {
        var json = """
            {
              "type": "FileLogEntry",
              "performedBy": {
                "type": "LogUser",
                "userName": "som user"
              }
            }
            """;
        var entry = JsonUtils.dtoObjectMapper.readValue(json, FileLogEntry.class);

        assertNotNull(entry.performedBy().username());
    }

    private static FileLogEntry randomFileLogEntry(Publication persistedPublication) {
        return FileLogEntry.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withFileIdentifier(SortableIdentifier.next())
                   .withResourceIdentifier(persistedPublication.getIdentifier())
                   .withTopic(LogTopic.FILE_UPLOADED)
                   .withTimestamp(Instant.now())
                   .withPerformedBy(randomLogUser())
                   .withFilename(randomString())
                   .build();
    }

    private static LogUser randomLogUser() {
        return new LogUser(randomString(), randomString(), randomString(), randomUri(),
                           new LogOrganization(randomUri(), randomUri(), randomString(),
                                               randomString()));
    }

    private static PublicationLogEntry randomLogEntry(SortableIdentifier resourceIdentifier, LogTopic logTopic) {
        return PublicationLogEntry.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withResourceIdentifier(resourceIdentifier)
                   .withTopic(logTopic)
                   .withTimestamp(Instant.now())
                   .withPerformedBy(randomLogUser())
                   .build();
    }
}