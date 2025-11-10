package no.unit.nva.publication.log.rest;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogOrganization;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.publicationstate.DoiRequestedEvent;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class FetchPublicationLogHandlerTest extends ResourcesLocalTest {

    private final Context context = new FakeContext();
    private ByteArrayOutputStream output;
    private FetchPublicationLogHandler handler;
    private ResourceService resourceService;
    private TicketService ticketService;

    @BeforeEach
    public void setUp() {
        super.init();
        output = new ByteArrayOutputStream();
        resourceService = getResourceService(client);
        ticketService = getTicketService();
        handler = new FetchPublicationLogHandler(resourceService, new Environment());
    }

    @Test
    void shouldReturnNotFoundWhenPublicationDoesNotExists() throws IOException {
        var publication = randomPublication();

        handler.handleRequest(createRequest(publication), output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthorized() throws IOException, BadRequestException {
        var publication = createPublishedPublication();

        handler.handleRequest(createUnauthorizedRequest(publication.getIdentifier()), output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturnForbiddenWhenUserHasNoRightsToFetchLog() throws IOException, BadRequestException {
        var publication = createPublishedPublication();

        handler.handleRequest(createRequest(publication), output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionThrown()
        throws IOException, BadRequestException, NotFoundException {
        var publication = createPublishedPublication();

        resourceService = mock(ResourceService.class);
        when(resourceService.getResourceByIdentifier(any())).thenReturn(Resource.fromPublication(publication));
        when(resourceService.getLogEntriesForResource(Resource.fromPublication(publication))).thenThrow(
            new RuntimeException());

        new FetchPublicationLogHandler(resourceService, new Environment()).handleRequest(
            createAuthorizedRequest(publication),
            output,
            context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_INTERNAL_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturnEmptyPublicationLogWhenUserHasRightsToFetchLogAndNoLogEntries()
        throws IOException, BadRequestException {
        var publication = createPublishedPublication();

        handler.handleRequest(createAuthorizedRequest(publication), output, context);

        var response = GatewayResponse.fromOutputStream(output, PublicationLogResponse.class);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertTrue(response.getBodyObject(PublicationLogResponse.class).logEntries().isEmpty());
    }

    @Test
    void shouldReturnNotEmptyPublicationLogWhenUserHasRightsToFetchLog() throws IOException, ApiGatewayException {
        var publication = createPublishedPublication();
        persistLogEntries(publication);
        handler.handleRequest(createAuthorizedRequest(publication), output, context);

        var response = GatewayResponse.fromOutputStream(output, PublicationLogResponse.class);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertFalse(response.getBodyObject(PublicationLogResponse.class).logEntries().isEmpty());
    }

    private void persistLogEntries(Publication publication) throws ApiGatewayException {
        var user = new LogUser(randomString(), randomUri(), randomString(), randomString(), randomString(),
                               randomString(), new LogOrganization(randomUri(), randomString(), Map.of()));
        Resource.resourceQueryObject(publication.getIdentifier())
            .fetch(resourceService)
            .orElseThrow()
            .getResourceEvent()
            .toLogEntry(publication.getIdentifier(), user)
            .persist(resourceService);

        var userInstance = UserInstance.fromPublication(publication);
        var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), userInstance);
        fileEntry.persist(resourceService, userInstance);
        fileEntry.getFileEvent().toLogEntry(fileEntry, user).persist(resourceService);

        var doiRequest = (DoiRequest) DoiRequest.create(Resource.fromPublication(publication), userInstance)
                                          .persistNewTicket(ticketService);
        doiRequest.setTicketEvent(DoiRequestedEvent.create(userInstance, Instant.now()));
        doiRequest.getTicketEvent()
            .toLogEntry(publication.getIdentifier(), doiRequest.getIdentifier(), user)
            .persist(resourceService);
    }

    private InputStream createRequest(Publication publication) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(dtoObjectMapper).withPathParameters(
                Map.of("publicationIdentifier", publication.getIdentifier().toString()))
                   .withUserName(randomString())
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .withAccessRights(randomUri())
                   .withCurrentCustomer(randomUri())
                   .build();
    }

    private InputStream createAuthorizedRequest(Publication publication) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(dtoObjectMapper).withPathParameters(
                Map.of("publicationIdentifier", publication.getIdentifier().toString()))
                   .withUserName(randomString())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .withPersonCristinId(randomUri())
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.MANAGE_RESOURCES_STANDARD)
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .build();
    }

    private Publication createPublishedPublication() throws BadRequestException {
        var publication = randomPublication(AcademicArticle.class);
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = resourceService.createPublication(userInstance, publication);
        return Resource.fromPublication(persistedPublication).publish(resourceService, userInstance).toPublication();
    }

    private InputStream createUnauthorizedRequest(SortableIdentifier identifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(dtoObjectMapper).withPathParameters(
            Map.of("publicationIdentifier", identifier.toString())).build();
    }
}
