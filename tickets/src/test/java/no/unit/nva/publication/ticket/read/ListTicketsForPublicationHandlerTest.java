package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.permissions.ticket.TicketPermissions;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.GeneralSupportRequestDto;
import no.unit.nva.publication.ticket.PublishingRequestDto;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketDtoParser;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ListTicketsForPublicationHandlerTest extends TicketTestLocal {

    private ListTicketsForPublicationHandler handler;
    private MessageService messageService;

    public static Stream<Arguments> accessRightAndTicketTypeProvider() {
        return Stream.of(Arguments.of(DoiRequestDto.class,
                                      new AccessRight[]{AccessRight.MANAGE_DOI, MANAGE_RESOURCES_STANDARD}),
                         Arguments.of(GeneralSupportRequestDto.class,
                                      new AccessRight[]{AccessRight.SUPPORT, MANAGE_RESOURCES_STANDARD}),
                         Arguments.of(PublishingRequestDto.class,
                                      new AccessRight[]{AccessRight.MANAGE_PUBLISHING_REQUESTS,
                                          MANAGE_RESOURCES_STANDARD}));
    }

    public static Stream<Arguments> accessRightAndTicketTypeProviderDraft() {
        return Stream.of(Arguments.of(PublishingRequestDto.class,
                                      new AccessRight[]{AccessRight.MANAGE_PUBLISHING_REQUESTS,
                                          MANAGE_RESOURCES_STANDARD}));
    }

    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new ListTicketsForPublicationHandler(resourceService, ticketService, new Environment());
        this.messageService = getMessageService();
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnAllTicketsForPublicationWhenUserIsThePublicationOwner(Class<? extends TicketEntry> ticketType,
                                                                           PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = ownerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        assertThatResponseContainsExpectedTickets(ticket);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnAllTicketsForPublicationWhenUserIsCurator(Class<? extends TicketEntry> ticketType,
                                                               PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = curatorRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        assertThatResponseContainsExpectedTickets(ticket);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotReturnRemovedTicket(Class<? extends TicketEntry> ticketType,
                                      PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.remove(UserInstance.fromTicket(ticket)).persistUpdate(ticketService);
        var request = ownerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), is(emptyIterable()));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnAllTicketsWithMessagesForPublicationWhenTicketContainsMessages(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var expectedTicketDto = constructDto(createPersistedTicketWithMessage(ticketType, publication));

        var request = ownerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        assertThat(body.getTickets(), containsInAnyOrder(expectedTicketDto));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnEmptyListForPublicationWhenUserIsNotTheOwnerAndNotElevatedUser(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = nonOwnerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        var body = response.getBodyObject(TicketCollection.class);
        assertThat(body.getTickets(), hasSize(0));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnEmptyListWhenUserIsElevatedUserOfAlienOrganization(Class<? extends TicketEntry> ticketType,
                                                                        PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = elevatedUserOfAlienOrgRequestsTicketsForPublication(publication);

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        var body = response.getBodyObject(TicketCollection.class);
        assertThat(body.getTickets(), hasSize(0));
    }

    @Test
    void shouldReturnTicketWhenUserHaveTransferAccess()
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);

        var request = curatorWithAccessRightRequestTicketsForPublication(publication,
                                                                         new AccessRight[]{
                                                                             AccessRight.MANAGE_PUBLISHING_REQUESTS,
                                                                             AccessRight.SUPPORT,
                                                                             AccessRight.MANAGE_RESOURCES_STANDARD});
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        var body = response.getBodyObject(TicketCollection.class);
        assertThat(body.getTickets(), hasSize(1));
    }

    @ParameterizedTest
    @MethodSource("accessRightAndTicketTypeProvider")
    void shouldListTicketsOfTypeCuratorHasAccessRightToOperateOn(
        Class<? extends TicketEntry> ticketType, AccessRight[] accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication);
        DoiRequest.create(Resource.fromPublication(publication), userInstance).persistNewTicket(ticketService);
        PublishingRequestCase.create(resource, userInstance, PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
            .persistNewTicket(ticketService);
        GeneralSupportRequest.create(resource, userInstance).persistNewTicket(ticketService);

        var request = curatorWithAccessRightRequestTicketsForPublication(publication, accessRight);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        var ticketsWithWriteAccess = body.getTickets()
                                         .stream()
                                         .filter(ticket -> ticket.getAllowedOperations()
                                                               .stream()
                                                               .anyMatch(a -> !a.equals(TicketOperation.READ)))
                                         .toList();

        assertThat(ticketsWithWriteAccess, hasSize(1));
        assertThat(ticketsWithWriteAccess.getFirst().getClass(), is(equalTo(ticketType)));
    }

    @ParameterizedTest
    @MethodSource("accessRightAndTicketTypeProviderDraft")
    void shouldListTicketsOfTypeCuratorHasAccessRightToOperateOnDraft(
        Class<? extends TicketEntry> ticketType, AccessRight[] accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);

        TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);

        var request = curatorWithAccessRightRequestTicketsForPublication(publication, accessRight);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), hasSize(1));
        assertThat(body.getTickets().getFirst().getClass(), is(equalTo(ticketType)));
    }

    @Test
    void shouldListCompletedDoiRequestAndPublishingRequestsTicketsForContributorAtAnotherInstitutionThanPublicationOwner()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var contributorId = randomUri();
        var contributor = randomContributorWithId(contributorId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        resourceService.updatePublication(publication);

        persistCompletedTicketsForPublication(publication,
                                              DoiRequest.class, PublishingRequestCase.class,
                                              GeneralSupportRequest.class);

        var request = userRequestsTickets(publication, contributorId, randomUri());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class).getTickets();

        assertThat(body, hasSize(2));

        var returnedTicketTypes = body.stream().map(TicketDto::ticketType).toList();

        assertThat(returnedTicketTypes, containsInAnyOrder(
            DoiRequest.class, PublishingRequestCase.class));
    }

    private void persistCompletedTicketsForPublication(Publication publication,
                                                       Class<? extends TicketEntry>... ticketTypes) {
        Arrays.stream(ticketTypes).forEach(ticketType -> {
            URI ownerAffiliation = randomUri();
            attempt(() -> TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService))
                .map(ticket -> ticket.withOwnerAffiliation(ownerAffiliation))
                .map(ticket -> ticket.complete(publication, randomUserInstance()))
                .forEach(ticketEntry -> ticketEntry.persistUpdate(ticketService));
        });
    }

    private UserInstance randomUserInstance() {
        return UserInstance.create(randomString(), randomUri());
    }

    @Test
    void shouldNotListPendingTicketsThatHasOtherReceivingOrganizationThanUserOrganization()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PublicationStatus.PUBLISHED,
                                                                           resourceService);
        var contributorId = randomUri();
        var contributor = randomContributorWithId(contributorId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        resourceService.updatePublication(publication);
        var userInstance = userInstanceWithTopLevelCristinOrg(randomUri());
        var resource = Resource.fromPublication(publication);
        URI channelOwnerOrganizationId = randomUri();
        FilesApprovalThesis.createForChannelOwningInstitution(resource, userInstance, channelOwnerOrganizationId,
                                                              SortableIdentifier.next(),
                                                              PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
            .persistNewTicket(ticketService);

        var request = userRequestsTickets(publication, contributorId, randomUri());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), is(emptyIterable()));
    }

    @Test
    void shouldNotListGeneralSupportRequestsThatHasOtherCuratingCustomerThanUserCustomer()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var contributorId = randomUri();
        var contributor = randomContributorWithId(contributorId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        resourceService.updatePublication(publication);
        GeneralSupportRequest.create(Resource.fromPublication(publication), UserInstance.fromPublication(publication))
            .persistNewTicket(ticketService)
            .complete(publication, randomUserInstance()).persistUpdate(ticketService);

        var request = userRequestsTickets(publication, contributorId, randomUri());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), is(emptyIterable()));
    }

    @Test
    void shouldListTicketsWhereUserIsOwner()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var contributorId = randomUri();
        var contributor = randomContributorWithId(contributorId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        resourceService.updatePublication(publication);
        var userIdentifier = randomString();
        GeneralSupportRequest.create(Resource.fromPublication(publication), UserInstance.create(userIdentifier,
                                                                                                randomUri(),
                                                                                                randomUri(),
                                                                                                List.of(),
                                                                                                contributor.getIdentity()
                                                                                                    .getId()))
            .persistNewTicket(ticketService)
            .complete(publication, randomUserInstance()).persistUpdate(ticketService);

        var request = userRequestsTickets(publication, userIdentifier, contributorId);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertFalse(body.getTickets().isEmpty());
    }

    @Test
    void shouldIncludeAllowedOperationsForTicketsWhenFileCurator()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PublicationStatus.PUBLISHED,
                                                                           resourceService);
        var user = UserInstance.fromPublication(publication);
        resourceService.updatePublication(publication);
        FilesApprovalThesis.createForUserInstitution(Resource.fromPublication(publication), user,
                                                     PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
            .persistNewTicket(ticketService)
            .complete(publication, user).persistUpdate(ticketService);

        var request = userRequestsTickets(publication, randomUri(), user.getTopLevelOrgCristinId(),
                                          MANAGE_PUBLISHING_REQUESTS, MANAGE_DEGREE, MANAGE_RESOURCES_STANDARD);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets().stream().findFirst().isPresent(), is(true));
    }

    @Test
    void shouldListTicketsForUserFromReceivingOrganization()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PublicationStatus.PUBLISHED,
                                                                           resourceService);
        var contributorId = randomUri();
        var contributor = randomContributorWithId(contributorId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        var channelOwnerOrganizationId = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(channelOwnerOrganizationId, Set.of())));
        resourceService.updatePublication(publication);
        var userInstance = UserInstance.create(randomString(), randomUri(), randomUri(),
                                               List.of(AccessRight.MANAGE_RESOURCE_FILES, MANAGE_DEGREE,
                                                       MANAGE_RESOURCES_STANDARD), channelOwnerOrganizationId);
        var resource = Resource.fromPublication(publication);
        FilesApprovalThesis.createForChannelOwningInstitution(resource, userInstance, channelOwnerOrganizationId,
                                                              SortableIdentifier.next(),
                                                              PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
            .persistNewTicket(ticketService);

        var request = userRequestsTickets(publication, contributorId, channelOwnerOrganizationId);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), is(not(emptyIterable())));
    }

    private TicketEntry createPersistedTicketWithMessage(Class<? extends TicketEntry> ticketType,
                                                         Publication publication) throws ApiGatewayException {
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        return ticket;
    }

    private TicketDto constructDto(TicketEntry ticketEntry) throws NotFoundException {
        var messages = ticketEntry.fetchMessages(ticketService);
        var resource = resourceService.getResourceByIdentifier(ticketEntry.getResourceIdentifier());
        var publicationPermissions = PublicationPermissions.create(resource, UserInstance.fromTicket(ticketEntry));
        var ticketPermissions = TicketPermissions.create(ticketEntry, UserInstance.fromTicket(ticketEntry),
                                                         resource, publicationPermissions);
        return TicketDto.fromTicket(ticketEntry, messages, getCuratingInstitutionsIdList(resource),
                                    ticketPermissions);
    }

    private static List<URI> getCuratingInstitutionsIdList(Resource resource) {
        return resource.getCuratingInstitutions().stream().map(CuratingInstitution::id).toList();
    }

    private static InputStream ownerRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(publication.getResourceOwner().getOwner().getValue())
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .build();
    }

    private static InputStream curatorRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(randomString())
                   .withPersonCristinId(randomUri())
                   .withAccessRights(publication.getPublisher().getId(), MANAGE_PUBLISHING_REQUESTS, MANAGE_DOI,
                                     SUPPORT, MANAGE_RESOURCES_STANDARD)
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .build();
    }

    private static InputStream nonOwnerRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(randomString())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private static Map<String, String> constructPathParameters(Publication publication) {
        return Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                      publication.getIdentifier().toString());
    }

    private InputStream elevatedUserOfAlienOrgRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(customerId)
                   .withUserName(randomString())
                   .withAccessRights(customerId, AccessRight.MANAGE_DOI)
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream curatorWithAccessRightRequestTicketsForPublication(Publication publication,
                                                                           AccessRight[] accessRight)
        throws JsonProcessingException {
        var customer = publication.getCuratingInstitutions()
                           .stream()
                           .map(CuratingInstitution::id)
                           .findFirst()
                           .orElseThrow();
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(customer)
                   .withUserName(randomString())
                   .withAccessRights(customer, accessRight)
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .build();
    }

    private void assertThatResponseContainsExpectedTickets(TicketEntry ticket) throws JsonProcessingException {
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        var actualTicketIdentifiers = body.getTickets()
                                          .stream()
                                          .map(TicketDtoParser::toTicket)
                                          .map(Entity::getIdentifier)
                                          .toList();
        var expectedIdentifiers = ticket.getIdentifier();
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(actualTicketIdentifiers, containsInAnyOrder(expectedIdentifiers));
    }

    private InputStream userRequestsTickets(Publication publication, URI userId, URI topLevelOrgCristinId,
                                            AccessRight... accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .withAccessRights(publication.getPublisher().getId(), accessRight)
                   .withPersonCristinId(userId)
                   .withTopLevelCristinOrgId(topLevelOrgCristinId)
                   .build();
    }

    private InputStream userRequestsTickets(Publication publication, String userName, URI userId)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(randomUri())
                   .withUserName(userName)
                   .withPersonCristinId(userId)
                   .withTopLevelCristinOrgId(randomUri())
                   .build();
    }
}