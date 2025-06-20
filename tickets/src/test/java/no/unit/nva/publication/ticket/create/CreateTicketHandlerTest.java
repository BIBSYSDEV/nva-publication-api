package no.unit.nva.publication.ticket.create;

import static com.github.tomakehurst.wiremock.common.ContentTypes.APPLICATION_JSON;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithIdAndAffiliation;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.RandomUtils.randomBackendUri;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.LOCATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.utils.CustomerList;
import no.unit.nva.publication.model.utils.CustomerSummary;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.GeneralSupportRequestDto;
import no.unit.nva.publication.ticket.MessageDto;
import no.unit.nva.publication.ticket.PublishingRequestDto;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class CreateTicketHandlerTest extends TicketTestLocal {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    private static final String PERSON_AFFILIATION_CLAIM = "custom:personAffiliation";

    private CreateTicketHandler handler;
    private TicketResolver ticketResolver;
    private MessageService messageService;

    public static Stream<Arguments> ticketEntryProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class)
                   .filter(type -> !type.getPayload().equals(UnpublishRequest.class))
                   .filter(type -> !type.getPayload().equals(FilesApprovalThesis.class))
                   .filter(type -> !type.getPayload().equals(PublishingRequestCase.class))
                   .map(Arguments::of);
    }

    @BeforeEach
    public void setup() {
        super.init();
        ticketResolver = new TicketResolver(resourceService, ticketService);
        messageService = new MessageService(client, new UriRetriever());
        this.handler = new CreateTicketHandler(ticketResolver, messageService, new Environment());
    }

    @ParameterizedTest
    @DisplayName("should persist ticket when publication exists, user is publication owner and "
                 + "publication meets ticket creation criteria")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldPersistTicketWhenPublicationExistsUserIsOwnerAndPublicationMeetsTicketCreationCriteria(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws IOException, ApiGatewayException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var owner = UserInstance.fromPublication(publication);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));

        var location = URI.create(response.getHeaders().get(LOCATION_HEADER));

        assertThatLocationHeaderHasExpectedFormat(publication, location);
        assertThatLocationHeaderPointsToCreatedTicket(location);
    }

    @ParameterizedTest(name = "should be possible to create DoiTicket for published publication")
    @EnumSource(value = PublicationStatus.class, names = {"PUBLISHED", "PUBLISHED_METADATA"})
    void shouldBePossibleToCreateDoiTicketForPublishedPublication(PublicationStatus publicationStatus)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(publicationStatus,
                                                                     resourceService);
        var requestBody = constructDto(DoiRequest.class);
        var owner = UserInstance.fromPublication(publication);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var location = URI.create(response.getHeaders().get(LOCATION_HEADER));
        assertThatLocationHeaderHasExpectedFormat(publication, location);
        assertThatLocationHeaderPointsToCreatedTicket(location);
    }

    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should should not allow creating a ticket for non existing publication")
    @MethodSource("ticketEntryProvider")
    void shouldNotAllowTicketCreationForNonExistingPublication(Class<? extends TicketEntry> ticketType)
        throws IOException {
        var publication = nonPersistedPublication();
        var requestBody = constructDto(ticketType);
        var owner = UserInstance.fromPublication(publication);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    @ParameterizedTest
    @DisplayName("should not allow users to create tickets for publications they do not own")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotAllowUsersToCreateTicketsForPublicationsTheyDoNotOwn(Class<? extends TicketEntry> ticketType,
                                                                       PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId(),
                                       randomUri(), List.of(), publication.getResourceOwner().getOwnerAffiliation());
        var input = createHttpTicketCreationRequest(requestBody, publication, user);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @DisplayName("should not allow users to create tickets for publications they do not belong to, i.e. "
                 + "where they are not listed as contributors, owner or curators for owner or contributors institution")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotAllowUsersToCreateTicketsForPublicationsBelongingToDifferentOrgThanTheOneTheyAreLoggedInTo(
        Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(randomString(), randomUri(), randomUri(), List.of(), randomUri());
        var input = createHttpTicketCreationRequest(requestBody, publication, user);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @DisplayName("should not allow anonymous users to create tickets")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotAllowAnonymousUsersToCreateTickets(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var input = createAnonymousHttpTicketCreationRequest(requestBody, publication);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as read for only the publication owner when publication owner creates new "
                 + "ticket")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotMarkTicketAsReadForThePublicationOwnerWhenPublicationOwnerCreatesNewTicket(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomBackendUri("customer"), status,
                                                                              resourceService);
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(ticketType);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        TicketEntry ticket = fetchTicket(response);
        assertTrue(ticket.getViewedBy().isEmpty());
    }

    @DisplayName("should update existing DoiRequest when new DOI is requested but a DoiRequest that has not been "
                 + "fulfilled already exists")
    @Test
    void shouldUpdateExistingDoiRequestWhenNewDoiIsRequestedButUnfulfilledDoiRequestAlreadyExists()
        throws ApiGatewayException, IOException {
        var publication = createPersistedNonDegreePublishedPublication();
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(DoiRequest.class);
        ticketResolver = new TicketResolver(resourceService, ticketService);
        this.handler = new CreateTicketHandler(ticketResolver, messageService, new Environment());
        var firstRequest = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(firstRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));

        final var createdTicket = fetchTicket(response).copy();
        final var secondRequest = createHttpTicketCreationRequest(requestBody, publication, owner);
        output = new ByteArrayOutputStream();
        ticketResolver = new TicketResolver(resourceService, ticketService);
        this.handler = new CreateTicketHandler(ticketResolver, messageService, new Environment());
        handler.handleRequest(secondRequest, output, CONTEXT);

        var secondResponse = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(secondResponse.getStatusCode(), is(equalTo(HTTP_CREATED)));

        var existingTicket = fetchTicket(secondResponse);
        assertThat(existingTicket.getModifiedDate(), is(greaterThan(createdTicket.getModifiedDate())));
    }

    @Test
    void shouldNotAllowUserWithManageDoiAccessRightToCreateDoiRequestForPublicationUserDoesNotBelongsTo()
        throws ApiGatewayException, IOException {
        var publication = createPersistedPublishedPublication();
        var requestBody = constructDto(DoiRequest.class);
        var request = createHttpTicketCreationRequestWithApprovedAccessRight(requestBody, publication,
                                                                             randomBackendUri("customer")
        );
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#invalidAccessRightForTicketTypeProvider")
    void shouldNotAllowCuratorWithoutValidAccessRightToCreateTicket(Class<? extends TicketEntry> ticketType,
                                                                    AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), DRAFT, resourceService);
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var input = createHttpTicketCreationRequestWithApprovedAccessRight(
            requestBody, publication, user.getUsername(), user.getCustomerId(), accessRights);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldAllowCuratorWithValidAccessRightToCreateTicket(PublicationStatus status,
                                                              Class<? extends TicketEntry> ticketType,
                                                                    AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), status, resourceService);
        var requestBody = constructDto(ticketType);
        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var input = createHttpTicketCreationRequest(
            requestBody, publication.getIdentifier(), curatingInstitution, randomUri(), randomString(), accessRights);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("ticketEntryProvider")
    void shouldAllowContributorToCreateTicketForNonDegreePublication(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED, resourceService);
        var requestBody = constructDto(ticketType);

        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var contributorCristinId = publication.getEntityDescription().getContributors().getFirst().getIdentity().getId();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, contributorCristinId, randomString());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldAllowCuratorWithValidAccessRightAndRelatedToContributorToCreateTicketForNonDegreePublication(
        PublicationStatus publicationStatus, Class<? extends TicketEntry> ticketType, AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), publicationStatus, resourceService);
        var requestBody = constructDto(ticketType);

        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, randomUri(), randomString(),accessRights);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("ticketEntryProvider")
    void shouldAllowContributorToCreateTicketForDegreePublication(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PUBLISHED, resourceService);
        var requestBody = constructDto(ticketType);

        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var contributorCristinId = publication.getEntityDescription().getContributors().getFirst().getIdentity().getId();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, contributorCristinId, randomString());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldAllowCuratorWithValidAccessRightAndRelatedContributorToCreateTicketForDegreePublication(
        PublicationStatus publicationStatus, Class<? extends TicketEntry> ticketType, AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(publicationStatus, resourceService);
        var requestBody = constructDto(ticketType);

        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, randomUri(), randomString(),accessRights);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldNotAllowNotRelatedCuratorWithValidAccessRightForNonDegreePublication(
        PublicationStatus publicationStatus, Class<? extends TicketEntry> ticketType, AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), publicationStatus,
                                                                              resourceService);
        var requestBody = constructDto(ticketType);

        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      randomUri(), randomUri(), randomString(),accessRights);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void contributorAtAnotherInstitutionThanPublicationOwnerShouldBeAbleToCreateTicketForPublication()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var contributorId = randomUri();
        var affiliationId = randomUri();
        publication.getEntityDescription().setContributors(List.of(randomContributorWithIdAndAffiliation(contributorId, affiliationId)));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(affiliationId, Set.of(contributorId))));
        mockCuratingInstitution(affiliationId);
        when(customerService.fetchCustomers()).thenReturn(new CustomerList(List.of(new CustomerSummary(randomUri(),
                                                                                                       affiliationId))));
        resourceService.updatePublication(publication);
        var requestBody = constructDto(DoiRequest.class);

        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      affiliationId, contributorId, randomString());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    private void mockCuratingInstitution(URI affiliationId) {
        when(uriRetriever.getRawContent(affiliationId, APPLICATION_JSON))
            .thenReturn(Optional.of("""
                                        {
                                          "type": "Organization",
                                          "partOf": [
                                            {
                                              "type": "Organization",
                                              "id": "%s"
                                            }
                                          ]
                                        }
                                        """.formatted(affiliationId)));
    }

    @Test
    void curatorAtAnotherInstitutionThanPublicationOwnerShouldBeAbleToCreateTicketForPublication()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of(randomUri()))));
        resourceService.updatePublication(publication);
        var requestBody = constructDto(GeneralSupportRequest.class);

        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, randomUri(), randomString(),
                                                      MANAGE_RESOURCES_STANDARD);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @Test
    void shouldPersistMessageWhenCreatingTicketWithMessage()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED,
                                                                              resourceService);
        var message = Message.builder().withText(randomString()).build();
        var requestBody = GeneralSupportRequestDto.builder()
                         .withMessages(List.of(MessageDto.fromMessage(message)))
                         .build(new GeneralSupportRequest());


        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      publication.getResourceOwner().getOwnerAffiliation(),
                                                      randomUri(), randomString(),
                                                      MANAGE_RESOURCES_STANDARD, SUPPORT);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var ticket = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                         .toList().getFirst();
        var persistedMessage = ticket.fetchMessages(ticketService).getFirst();

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(persistedMessage.getText(), is(equalTo(message.getText())));
    }

    @Test
    void shouldPersistSingleMessageWhenCreatingTicketWithMultipleMessage()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED,
                                                                              resourceService);
        var firstMessage = Message.builder().withText(randomString()).build();
        var secondMessage = Message.builder().withText(randomString()).build();
        var requestBody = GeneralSupportRequestDto.builder()
                              .withMessages(List.of(MessageDto.fromMessage(firstMessage),
                                                    MessageDto.fromMessage(secondMessage)))
                              .build(new GeneralSupportRequest());


        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      publication.getResourceOwner().getOwnerAffiliation(),
                                                      randomUri(), randomString(),
                                                      MANAGE_RESOURCES_STANDARD, SUPPORT);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var ticket = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                         .toList().getFirst();
        var persistedMessage = ticket.fetchMessages(ticketService);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(persistedMessage, hasSize(1));
    }

    @Test
    void creatingTicketWithoutMessageShouldNotLogAnyMessages()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED,
                                                                              resourceService);

        var request = createHttpTicketCreationRequest(
            constructDto(GeneralSupportRequest.class), publication.getIdentifier(),
            publication.getResourceOwner().getOwnerAffiliation(), randomUri(), randomString(),
            MANAGE_RESOURCES_STANDARD, SUPPORT);
        var logAppender = LogUtils.getTestingAppender(CreateTicketHandler.class);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var logMessages = logAppender.getMessages();

        assertThat(logMessages, is(emptyString()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("ticketEntryProvider")
    void shouldSetUserFromRequestAsTicketOwner(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED, resourceService);
        var requestBody = constructDto(ticketType);
        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();

        var userId = publication.getEntityDescription().getContributors().getFirst().getIdentity().getId();
        var username = randomString();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, userId, username);
        handler.handleRequest(request, output, CONTEXT);

        var ticket =
            resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication)).toList().getFirst();

        assertThat(ticket.getOwner().toString(), is(equalTo(username)));
    }

    private TicketEntry fetchTicket(GatewayResponse<Void> response) throws NotFoundException {
        var ticketIdentifier = new SortableIdentifier(UriWrapper.fromUri(response.getHeaders().get(LOCATION_HEADER))
                                                          .getLastPathElement());
        return ticketService.fetchTicketByIdentifier(ticketIdentifier);
    }

    private static SortableIdentifier extractTicketIdentifierFromLocation(URI location) {
        return new SortableIdentifier(UriWrapper.fromUri(location).getLastPathElement());
    }

    private static void assertThatLocationHeaderHasExpectedFormat(Publication publication, URI location) {
        var ticketIdentifier = extractTicketIdentifierFromLocation(location);
        var publicationIdentifier = extractPublicationIdentifierFromLocation(location);
        assertThat(publicationIdentifier, is(equalTo(publication.getIdentifier())));
        assertThat(ticketIdentifier, is(not(nullValue())));
    }

    private static SortableIdentifier extractPublicationIdentifierFromLocation(URI location) {
        return UriWrapper.fromUri(location)
                   .getParent()
                   .flatMap(UriWrapper::getParent)
                   .map(UriWrapper::getLastPathElement)
                   .map(SortableIdentifier::new)
                   .orElseThrow();
    }

    private void assertThatLocationHeaderPointsToCreatedTicket(URI ticketUri)
        throws NotFoundException {
        var publication = fetchPublication(ticketUri);
        var ticketIdentifier = extractTicketIdentifierFromLocation(ticketUri);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);
        assertThat(ticket.getResourceIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(ticket.getIdentifier(), is(equalTo(ticketIdentifier)));
    }

    private Publication fetchPublication(URI ticketUri) throws NotFoundException {
        var publicationIdentifier = extractPublicationIdentifierFromLocation(ticketUri);
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private TicketDto constructDto(Class<? extends TicketEntry> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequestDto.empty();
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return PublishingRequestDto.empty();
        } else if (GeneralSupportRequest.class.equals(ticketType)) {
            return GeneralSupportRequestDto.empty();
        }
        throw new RuntimeException("Unrecognized ticket type");
    }

    private Publication createPersistedPublishedPublication() throws ApiGatewayException {
        var publication = randomPublication();
        publication.setDoi(null); // for creating DoiRequests
        publication = Resource.fromPublication(publication)
                          .persistNew(resourceService, UserInstance.fromPublication(publication));
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        return resourceService.getPublicationByIdentifier(publication.getIdentifier());
    }

    private Publication createPersistedNonDegreePublishedPublication() throws ApiGatewayException {
        var publication = randomNonDegreePublication();
        publication.setDoi(null); // for creating DoiRequests
        publication = Resource.fromPublication(publication)
                          .persistNew(resourceService, UserInstance.fromPublication(publication));
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        return resourceService.getPublicationByIdentifier(publication.getIdentifier());
    }

    private InputStream createHttpTicketCreationRequest(TicketDto ticketDto,
                                                        Publication publication,
                                                        UserInstance userCredentials)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withAuthorizerClaim(PERSON_AFFILIATION_CLAIM, userCredentials.getUsername())
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withUserName(userCredentials.getUsername())
                   .withCurrentCustomer(userCredentials.getCustomerId())
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(userCredentials.getTopLevelOrgCristinId())
                   .build();
    }

    private InputStream createHttpTicketCreationRequest(TicketDto ticketDto,
                                                        SortableIdentifier publicationIdentifier,
                                                        URI topLevelCristinOrganizationId,
                                                        URI userCristinId,
                                                        String username,
                                                        AccessRight... accessRights)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString()))
                   .withUserName(username)
                   .withCurrentCustomer(randomBackendUri("customer"))
                   .withPersonCristinId(userCristinId)
                   .withTopLevelCristinOrgId(topLevelCristinOrganizationId)
                   .withAccessRights(randomUri(), accessRights)
                   .build();
    }

    private InputStream createHttpTicketCreationRequestWithApprovedAccessRight(TicketDto ticketDto,
                                                                               Publication publication,
                                                                               URI customerId)
        throws JsonProcessingException {
        return createHttpTicketCreationRequestWithApprovedAccessRight(ticketDto,
                                                                      publication,
                                                                      randomString(),
                                                                      customerId,
                                                                      AccessRight.MANAGE_DOI);
    }

    private InputStream createHttpTicketCreationRequestWithApprovedAccessRight(TicketDto ticketDto,
                                                                               Publication publication,
                                                                               String userName,
                                                                               URI customerId,
                                                                               AccessRight... accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withAuthorizerClaim(PERSON_AFFILIATION_CLAIM, customerId.toString())
                   .withAccessRights(customerId, accessRight)
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withUserName(userName)
                   .withCurrentCustomer(customerId)
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream createAnonymousHttpTicketCreationRequest(TicketDto ticketDto,
                                                                 Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .build();
    }
}
