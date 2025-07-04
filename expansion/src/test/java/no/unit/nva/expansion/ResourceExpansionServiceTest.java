package no.unit.nva.expansion;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static no.unit.nva.expansion.model.ExpandedTicket.extractIdentifier;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFileWithLicense;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.expansion.model.ExpandedPublishingRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.expansion.model.ExpandedTicketStatus;
import no.unit.nva.expansion.model.ExpandedUnpublishRequest;
import no.unit.nva.expansion.model.ExpansionException;
import no.unit.nva.expansion.model.License;
import no.unit.nva.expansion.model.License.LicenseType;
import no.unit.nva.expansion.model.nvi.ScientificIndex;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.RelationType;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.FakeSqsClient;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.publication.uriretriever.FakeUriResponse;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceExpansionServiceTest extends ResourcesLocalTest {

    public static final URI ORGANIZATION =
        URI.create("https://api.dev.nva.aws.unit.no/cristin/person/myCristinId/myOrganization");
    public static final UserInstance USER = UserInstance.create("12345", ORGANIZATION, ORGANIZATION,
                                                                List.of(), ORGANIZATION);
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String TICKET_EVENT = "ticketEvent";
    private static final String VIEWED_BY = "viewedBy";
    private static final String WORKFLOW = "workflow";
    private static final String ASSIGNEE = "assignee";
    private static final String OWNER_AFFILIATION = "ownerAffiliation";
    private static final String FINALIZED_BY = "finalizedBy";
    public static final String APPROVED_FILES = "approvedFiles";
    public static final String FILES_FOR_APPROVAL = "filesForApproval";
    public static final String RESPONSIBILITY_AREA = "responsibilityArea";

    private ResourceExpansionService expansionService;
    private ResourceService resourceService;
    private MessageService messageService;
    private TicketService ticketService;
    private FakeUriRetriever fakeUriRetriever;
    private FakeSqsClient sqsClient;

    public static Stream<Named<Class<?>>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    @BeforeEach
    void setUp() {
        super.init();
        initializeServices();
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingTheOrganizationOfTheOwnersResponsibilityAreaAsIs(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws Exception {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var randomReceivingOrganizationId = PublicationGenerator.randomUri();
        ticket.setReceivingOrganizationDetailsAndResetAssignee(randomReceivingOrganizationId,
                                                               randomReceivingOrganizationId);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket, false).orElseThrow();
        assertThat(expandedTicket.getOrganization().id(),
                   is(equalTo(ticket.getReceivingOrganizationDetails().subOrganizationId())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingFinalizedByValue(Class<? extends TicketEntry> ticketType,
                                                              PublicationStatus status)
        throws ApiGatewayException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setResponsibilityArea(PublicationGenerator.randomUri());
        ticket.setFinalizedBy(new Username(randomString()));
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        expansionService = mockedExpansionService();

        var finalizedBy = ticket.getFinalizedBy().getValue();
        var expectedExpandedFinalizedBy = getExpectedExpandedPerson(new User(finalizedBy));
        var expandedFinalizedBy = expansionService.expandPerson(new User(finalizedBy));
        assertThat(expandedFinalizedBy, is(equalTo(expectedExpandedFinalizedBy)));
    }

    @DisplayName("should copy all publicly visible fields from Ticket to ExpandedTicket")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldCopyAllPubliclyVisibleFieldsFromTicketToExpandedTicket(Class<? extends TicketEntry> ticketType,
                                                                      PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket, false).orElseThrow();

        assertThat(ticket,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(WORKFLOW, ASSIGNEE, FINALIZED_BY,
                                                               FINALIZED_DATE, OWNER_AFFILIATION, APPROVED_FILES,
                                                               FILES_FOR_APPROVAL,
                                                               RESPONSIBILITY_AREA, TICKET_EVENT, VIEWED_BY)));
        var expectedPublicationId = constructExpectedPublicationId(publication);
        assertThat(expandedTicket.getPublication().getPublicationId(), is(equalTo(expectedPublicationId)));
    }

    @DisplayName("should update associated Ticket when a Message is created")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldExpandAssociatedTicketWhenMessageIsCreated(Class<? extends TicketEntry> ticketType,
                                                          PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket, false).orElseThrow();
        var messages = expandedTicket.getMessages();
        var expectedExpandedMessage = messageToExpandedMessage(message);
        assertThat(messages, contains(expectedExpandedMessage));
    }

    @ParameterizedTest(name = "should return framed index document for resources. Instance type:{0}")
    @MethodSource("listPublicationInstanceTypes")
    void shouldReturnFramedIndexDocumentFromResource(Class<?> instanceType)
        throws JsonProcessingException, NotFoundException, BadRequestException {

        var publication = PublicationGenerator.randomPublication(instanceType)
                              .copy()
                              .withEntityDescription(new EntityDescription())
                              .build();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);

        var resourceUpdate = Resource.fromPublication(resource);
        var indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate, false)
                                              .orElseThrow();
        assertThat(indexDoc.fetchId(), is(not(nullValue())));
    }

    @ParameterizedTest(name = "should return framed index document containing license:{0}")
    @MethodSource("licenseProvider")
    void shouldReturnIndexDocumentContainingLicense(String licenseUri, LicenseType expectedLicense)
        throws JsonProcessingException, NotFoundException, BadRequestException {
        var fileWithLicense = randomOpenFileWithLicense(URI.create(licenseUri));
        var associatedLink = new AssociatedLink(randomUri(), null, null, RelationType.SAME_AS);
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class)
                              .copy()
                              .withAssociatedArtifacts(List.of(fileWithLicense, associatedLink))
                              .build();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);

        var resourceUpdate = Resource.fromPublication(resource);
        var indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate, false).orElseThrow();
        var licensesAsString = getLicenseForFile(indexDoc);
        var license = JsonUtils.dtoObjectMapper.readValue(licensesAsString, License.class);

        assertThat(license.name(), is(equalTo(expectedLicense.toLicense(URI.create(licenseUri)).name())));
    }

    @Test
    void shouldReturnIndexDocumentWithoutLicenseWhenNoLicense()
        throws JsonProcessingException, NotFoundException, BadRequestException {
        var fileWithoutLicense = File.builder().withIdentifier(randomUUID()).buildOpenFile();
        var link = new AssociatedLink(randomUri(), null, null, RelationType.SAME_AS);
        var publication = PublicationGenerator.randomPublication()
                              .copy()
                              .withAssociatedArtifacts(List.of(fileWithoutLicense, link))
                              .build();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);
        var resourceUpdate = Resource.fromPublication(resource);
        var indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate, false).orElseThrow();
        var license = indexDoc.asJsonNode().get("associatedArtifacts").get(0).get("license");
        assertThat(license, is(nullValue()));
    }

    private static String getLicenseForFile(ExpandedResource indexDoc) {
        var associatedArtifacts = indexDoc.asJsonNode().get("associatedArtifacts");
        String string = null;
        for (JsonNode artifact : associatedArtifacts) {
            if (!artifact.get("type").asText().equals("AssociatedLink")) {
                var license = artifact.get("license");
                string = nonNull(license) ? license.toString() : null;
            }
        }
        return string;
    }

    @Test
    void shouldReturnIndexDocumentWithContextUri()
        throws JsonProcessingException, NotFoundException, BadRequestException {

        var publication = randomPublication(AcademicArticle.class)
                              .copy()
                              .withEntityDescription(new EntityDescription())
                              .build();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);

        var indexDoc = (ExpandedResource) expansionService.expandEntry(Resource.fromPublication(resource), true)
                                              .orElseThrow();
        assertThat(indexDoc.getAllFields().get("@context"),
                   is(equalTo("https://api.dev.nva.aws.unit.no/publication/context")));
    }

    @Test
    void shouldIncludedOnlyMessagesAssociatedToExpandedTicket() throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var owner = UserInstance.fromPublication(publication);

        var ticketToBeExpanded = TicketEntry.requestNewTicket(publication, GeneralSupportRequest.class)
                                     .withOwner(UserInstance.fromPublication(publication).getUsername())
                                     .persistNewTicket(ticketService);
        FakeUriResponse.setupFakeForType(ticketToBeExpanded, fakeUriRetriever);

        var message = messageService.createMessage(ticketToBeExpanded, owner, randomString());
        var expectedExpandedMessage = messageToExpandedMessage(message);
        var unexpectedMessages = messagesOfDifferentTickets(publication, owner, GeneralSupportRequest.class);
        var expandedEntry = (ExpandedTicket) expansionService.expandEntry(ticketToBeExpanded, false).orElseThrow();
        assertThat(expandedEntry.getMessages(), hasItem(expectedExpandedMessage));
        assertThat(unexpectedMessages, everyItem(not(in(expandedEntry.getMessages()))));
    }

    @Test
    void shouldExpandAssociatedTicketAndNotTheMessageItselfWhenNewMessageArrivesForExpansion()
        throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var owner = UserInstance.fromPublication(publication);

        var generalSupportRequest = GeneralSupportRequest.create(Resource.fromPublication(publication),
                                                                 UserInstance.fromPublication(publication));
        generalSupportRequest.setViewedBy(Set.of(owner.getUser()));
        var ticketToBeExpanded = generalSupportRequest.persistNewTicket(ticketService);

        FakeUriResponse.setupFakeForType(ticketToBeExpanded, fakeUriRetriever);

        var messageThatWillLeadToTicketExpansion = messageService.createMessage(ticketToBeExpanded, owner,
                                                                                randomString());
        var expectedExpandedMessage = messageToExpandedMessage(messageThatWillLeadToTicketExpansion);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(messageThatWillLeadToTicketExpansion, false)
                                                  .orElseThrow();
        assertThat(expandedTicket.getMessages(), hasItem(expectedExpandedMessage));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldAddResourceTitleToExpandedTicket(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket, false).orElseThrow();

        var expectedTitle = publication.getEntityDescription().getMainTitle();
        assertThat(expandedTicket.getPublication().getTitle(), is(equalTo(expectedTitle)));
    }

    @Test
    void shouldThrowIfUnsupportedType() {
        var unsupportedImplementation = mock(Entity.class);
        assertThrows(UnsupportedOperationException.class,
                     () -> expansionService.expandEntry(unsupportedImplementation, false));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUseOwnerAffiliationWhenTicketHasOwnerAffiliation(Class<? extends TicketEntry> ticketType,
                                                                PublicationStatus status)
        throws Exception {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        var expectedOrgId = ticket.getReceivingOrganizationDetails().subOrganizationId();
        var actualAffiliation = expansionService.getOrganization(ticket).id();
        assertThat(actualAffiliation, is(equalTo(expectedOrgId)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUseResourceOwnerAffiliationWhenTicketHasNoOwnerAffiliation(Class<? extends TicketEntry> ticketType,
                                                                          PublicationStatus status) throws Exception {
        var userInstance = new UserInstance(randomString(), randomUri(), randomUri(), randomUri(), null, null,
                                            UserClientType.EXTERNAL);
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, userInstance, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        var expectedOrgId = publication.getResourceOwner().getOwnerAffiliation();
        var actualAffiliation = expansionService.getOrganization(ticket).id();
        assertThat(actualAffiliation, is(equalTo(expectedOrgId)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldGetOrganizationIdentifierForAffiliations(Class<? extends TicketEntry> ticketType,
                                                        PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var publicationOwnerAffiliationId = publication.getResourceOwner().getOwnerAffiliation();
        var expectedIdentifier = UriWrapper.fromUri(publicationOwnerAffiliationId).getLastPathElement();
        var actualIdentifier = expansionService.getOrganization(ticket).identifier();

        assertThat(actualIdentifier, is(equalTo(expectedIdentifier)));
    }

    @Test
    void shouldReturnNullIfNotTicketEntry() throws NotFoundException {
        var message = Message.builder()
                          .withResourceIdentifier(SortableIdentifier.next())
                          .withTicketIdentifier(SortableIdentifier.next())
                          .withIdentifier(SortableIdentifier.next())
                          .build();

        var actual = expansionService.getOrganization(message);

        assertThat(actual, is(nullValue()));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldExpandTicketOwner(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, USER, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        expansionService = mockedExpansionService();

        var ticketOwner = ticket.getOwner();
        var expandedOwner = expansionService.expandPerson(ticketOwner);
        var expectedExpandedOwner = getExpectedExpandedPerson(ticketOwner);
        assertThat(expandedOwner, is(equalTo(expectedExpandedOwner)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldExpandAssigneeWhenPresent(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, USER, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = ticketWithAssignee(ticketType, publication);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);
        expansionService = mockedExpansionService();

        var assignee = ticket.getAssignee().getValue();
        var expectedExpandedAssignee = getExpectedExpandedPerson(new User(assignee));
        var expandedAssignee = expansionService.expandPerson(new User(assignee));
        assertThat(expandedAssignee, is(equalTo(expectedExpandedAssignee)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldCopyAllPubliclyVisibleFieldsFromMessageToExpandedMessage(Class<? extends TicketEntry> ticketType,
                                                                        PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        var expandedMessage = expansionService.expandMessage(message);
        var regeneratedMessage = expandedMessage.toMessage();
        assertThat(regeneratedMessage, is(equalTo(message)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Resource", "TicketEntry", "Message"})
    void shouldLogTypeAndIdentifierWhenEntityIsExpanded(String type)
        throws ApiGatewayException, JsonProcessingException {
        final var logAppender = LogUtils.getTestingAppender(ResourceExpansionServiceImpl.class);

        var entity = findEntity(type);

        expansionService.expandEntry(entity, false);

        assertThat(logAppender.getMessages(), containsString(type + ": " + entity.getIdentifier().toString()));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusNewWhenTicketStatusIsPendingWithoutAssignee(
        Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket, false).orElseThrow();
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.NEW)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusClosedWhenTicketStatusIsClosed(Class<? extends TicketEntry> ticketType,
                                                                        PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        ticket.setStatus(TicketStatus.CLOSED);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket, false).orElseThrow();
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.CLOSED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusClosedWhenTicketStatusIsCompleted(Class<? extends TicketEntry> ticketType,
                                                                           PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        ticket.setStatus(TicketStatus.COMPLETED);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket, false).orElseThrow();
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.COMPLETED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingViewedByValue(Class<? extends TicketEntry> ticketType,
                                                           PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        var viewedBySet = Set.of(new User(randomString()));
        ticket.setViewedBy(viewedBySet);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        expansionService = mockedExpansionService();

        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket, false).orElseThrow();
        var viewedBy = ticket.getViewedBy();
        var expectedExpandedViewedBy = getExpectedExpandedPerson(new User(viewedBy.toString()));
        assert expandedTicket.getViewedBy().stream()
                   .map(ExpandedPerson::username)
                   .anyMatch(i -> i.equals(expectedExpandedViewedBy.username()));
        var expectedExpandedViewedBySet = Set.of(expectedExpandedViewedBy);
        assertThat(expandedTicket.getViewedBy(), is(equalTo(expectedExpandedViewedBySet)));
    }

    @Test
    void shouldReturnExpandedResourceWithEntityDescriptionForPublicationWithContributorWithoutId()
        throws JsonProcessingException, NotFoundException, BadRequestException {
        var publicationJsonString = stringFromResources(Path.of("publication_without_contributor_id_sample.json"));

        var publication = objectMapper.readValue(publicationJsonString, Publication.class);
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);
        var resourceUpdate = Resource.fromPublication(resource);

        expansionService = mockedExpansionService();
        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate, false).orElseThrow();
        var actualEntityDescription = expandedResource.asJsonNode().at("/entityDescription").toString();

        assertThat(actualEntityDescription, is(not(equalTo(""))));
    }

    @Test
    void shouldReturnExpandedResourceWithContributorForPublicationWithContributorWithoutIdOrAffiliation()
        throws JsonProcessingException, NotFoundException, BadRequestException {
        var contributorWithoutIdOrAffiliation = createContributor(randomElement(Role.values()), null, randomString(),
                                                                  Collections.emptyList(), 1);
        var entityDescription = createEntityDescriptionWithContributor(contributorWithoutIdOrAffiliation);
        var publication = PublicationGenerator.randomPublication()
                              .copy()
                              .withEntityDescription(entityDescription)
                              .build();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);
        var resourceUpdate = Resource.fromPublication(resource);

        expansionService = mockedExpansionService();
        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate, false).orElseThrow();
        var actualContributors = extractContributors(expandedResource);
        assertThat(actualContributors, containsInAnyOrder(List.of(contributorWithoutIdOrAffiliation).toArray()));
    }

    @ParameterizedTest
    @MethodSource("publicationInstanceProvider")
    void shouldReturnDataThatIsFullyDefinedInJsonLdContext(Class<?> instanceType)
        throws NotFoundException, JsonProcessingException, BadRequestException {
        var publication = PublicationGenerator.randomPublication(instanceType);
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);
        expansionService = mockedExpansionService();
        var expandedResource = (ExpandedResource) expansionService.expandEntry(Resource.fromPublication(resource),
                                                                               false)
                                                      .orElseThrow();
        var json = expandedResource.asJsonNode();
        json.remove("@context");
        JsonPropertyScraper.getAllProperties(json).forEach(item -> assertFalse(item.contains("http")));
        JsonPropertyScraper.getAllProperties(json).forEach(item -> assertFalse(item.contains("@")));
    }

    @Test
    void shouldReturnExpandedResourceWithCorrectNumberOfContributorsForPublicationWithSamePersonInDifferentRoles()
        throws JsonProcessingException, NotFoundException, BadRequestException {
        var id = randomUri();
        var publication = getPublicationWithSamePersonInDifferentContributorRoles(id);
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);
        var resourceUpdate = Resource.fromPublication(resource);

        expansionService = mockedExpansionService();
        var expandedResourceAsJson = expansionService.expandEntry(resourceUpdate, false).orElseThrow().toJsonString();

        var regeneratedPublication = objectMapper.readValue(expandedResourceAsJson, Publication.class);

        var contributorsWithSameId = extractContributorsWithId(id, regeneratedPublication);
        assertThat(contributorsWithSameId.size(), is(equalTo(2)));
    }

    @Test
    void shouldExpandApprovedFilesForPublishingRequest()
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(PUBLISHED, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = createCompletedTicketAndPublishFiles(publication);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);
        var expandedTicket = (ExpandedPublishingRequest) expansionService.expandEntry(ticket, false).orElseThrow();

        var publishedFilesFromPublication = publication
                                                .getAssociatedArtifacts().stream()
                                                .map(File.class::cast)
                                                .map(File::getIdentifier)
                                                .collect(Collectors.toSet());
        var publishedFilesFromExpandedPublishingRequest = expandedTicket.getApprovedFiles()
                                                              .stream().map(File::getIdentifier).toList();

        assertThat(publishedFilesFromPublication,
                   containsInAnyOrder(publishedFilesFromExpandedPublishingRequest.toArray()));
    }

    @Test
    void shouldExpandFilesForApprovalForPublishingRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(PUBLISHED, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var ticket = persistPublishingRequestContainingExistingUnpublishedFiles(publication);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);
        var expandedTicket = (ExpandedPublishingRequest) expansionService.expandEntry(ticket, false).orElseThrow();
        var regeneratedTicket = (PublishingRequestCase) toTicketEntry(expandedTicket);

        assertThat(regeneratedTicket, is(equalTo(ticket)));

        var expectedFilesForApproval = resourceService.getPublicationByIdentifier(publication.getIdentifier())
                                           .getAssociatedArtifacts().stream()
                                           .filter(PendingOpenFile.class::isInstance)
                                           .toArray();
        var filesForApproval = expandedTicket.getFilesForApproval();

        assertThat(filesForApproval, containsInAnyOrder(expectedFilesForApproval));
    }

    @Test
    void shouldExpandPublicationWithNviStatusWhenPublicationIsReportedNviCandidate()
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var resourceUpdate = Resource.fromPublication(publication);
        var expansionService = expansionServiceReturningNviCandidate(publication, nviCandidateResponse(), 200);
        var expandedResourceAsJson = expansionService.expandEntry(resourceUpdate, false)
                                         .orElseThrow()
                                         .toJsonString();
        var json = JsonUtils.dtoObjectMapper.readTree(expandedResourceAsJson);
        var nviStatusNode = json.get(ScientificIndex.SCIENTIFIC_INDEX_FIELD);

        assertThat(nviStatusNode.get("year").asText(), is(equalTo("2024")));
        assertThat(nviStatusNode.get("status").asText(), is(equalTo("Reported")));
    }

    @Test
    void shouldNotAddNviStatusToPublicationWhenNotFoundResponseFromNvi()
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var resourceUpdate = Resource.fromPublication(publication);
        var expansionService = expansionServiceReturningNviCandidate(publication, nviCandidateResponse(), 404);
        var expandedResourceAsJson = expansionService.expandEntry(resourceUpdate, false)
                                         .orElseThrow()
                                         .toJsonString();
        var json = JsonUtils.dtoObjectMapper.readTree(expandedResourceAsJson);

        assertThat(json.get("nviStatus"), is(nullValue()));
    }

    @Test
    void doiShouldBeExpandedAsUriWhenMultipleDoiOccurrencesInPublication()
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublicationWithDoi(PUBLISHED, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var doi = publication.getEntityDescription().getReference().getDoi();
        publication.getAssociatedArtifacts()
            .add(new AssociatedLink(doi, randomString(), randomString(), RelationType.SAME_AS));
        var publicationWithIdenticalDoiValues = resourceService.updatePublication(publication);
        var resourceUpdate = Resource.fromPublication(publicationWithIdenticalDoiValues);
        var expansionService = expansionServiceReturningNviCandidate(publication, nviCandidateResponse(), 404);
        var expandedResourceAsJson = expansionService.expandEntry(resourceUpdate, false)
                                         .orElseThrow().toJsonString();
        var json = JsonUtils.dtoObjectMapper.readTree(expandedResourceAsJson);

        var expandedDoi = json.at("/entityDescription/reference/doi");
        assertThat(expandedDoi.asText(), is(equalTo(doi.toString())));
    }

    @Test
    void shouldExpandResourceOnFileEntryUpdateWhenPresentInDatabase()
        throws BadRequestException, NotFoundException, JsonProcessingException {
        var publication = randomPublication(ExhibitionProduction.class);
        publication.setStatus(PUBLISHED);

        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, false);

        var fileEntry = FileEntry.create(openFileFromPublication(publication), persistedPublication.getIdentifier(),
                                         userInstance);
        var expandedResourceAsJson = expansionService.expandEntry(fileEntry, false)
                                         .orElseThrow().toJsonString();
        var expandedResource = JsonUtils.dtoObjectMapper.readTree(expandedResourceAsJson);
        assertThat(expandedResource.at("/identifier").asText(),
                   is(equalTo(persistedPublication.getIdentifier().toString())));
    }

    @Test
    void shouldNotExpandResourceOnFileEntryUpdateWhenNotPresentInDatabase()
        throws BadRequestException, NotFoundException, JsonProcessingException {
        var publication = randomPublication(ExhibitionProduction.class);
        publication.setStatus(DRAFT);

        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, false);

        resourceService.deleteDraftPublication(userInstance, persistedPublication.getIdentifier());

        var fileEntry = FileEntry.create(openFileFromPublication(publication), persistedPublication.getIdentifier(),
                                         userInstance);
        var result = expansionService.expandEntry(fileEntry, false);

        assertThat(result.isPresent(), is(false));
    }

    private File openFileFromPublication(Publication publication) {
        return publication.getAssociatedArtifacts()
                   .stream()
                   .filter(OpenFile.class::isInstance)
                   .map(OpenFile.class::cast)
                   .findFirst().orElseThrow();
    }

    @Test
    void shouldThrowExpansionExceptionWhenUnexpectedErrorFetchingNviCandidateForPublication()
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var resourceUpdate = Resource.fromPublication(publication);
        var expansionService = expansionServiceReturningNviCandidate(publication, randomString(), 200);

        assertThrows(ExpansionException.class, () -> expansionService.expandEntry(resourceUpdate, false));
    }

    @Test
    void shouldExpandUnpublishRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        resourceService.unpublishPublication(publication, UserInstance.fromPublication(publication));
        var ticket = TicketTestUtils.createPersistedTicket(publication, UnpublishRequest.class, ticketService);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var expandedTicket = expansionService.expandEntry(ticket, false);

        assertThat(expandedTicket.orElseThrow(), instanceOf(ExpandedUnpublishRequest.class));
    }

    // TODO: Handle redirects where id's received back in response matches with the requested resource
    @Test
    void shouldThrowExpansionExceptionWhenFetchingPublicationChannelReturnsRedirect()
        throws ApiGatewayException {
        var publication = randomPublication(JournalArticle.class);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService,
                                                                                    UserInstance.fromPublication(
                                                                                        publication));
        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, true);

        assertThrows(RuntimeException.class,
                     () -> expansionService.expandEntry(Resource.fromPublication(persistedPublication),
                                                        false));
    }

    private ResourceExpansionServiceImpl expansionServiceReturningNviCandidate(Publication publication,
                                                                               String responseBody, int statusCode) {

        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        FakeUriResponse.setUpNviResponse(fakeUriRetriever, statusCode, publication, responseBody);

        return new ResourceExpansionServiceImpl(resourceService,
                                                new TicketService(client, fakeUriRetriever),
                                                fakeUriRetriever,
                                                fakeUriRetriever, sqsClient);
    }

    private String nviCandidateResponse() {
        return """
            {
                "reportStatus": {
                    "status": "REPORTED",
                    "description": "Reported in closed period"
                },
                "period": "2024"
            }
            """;
    }

    private TicketEntry createCompletedTicketAndPublishFiles(Publication publication) throws ApiGatewayException {
        var ticket = (PublishingRequestCase) TicketTestUtils.createCompletedTicket(
            publication, PublishingRequestCase.class, ticketService);
        ticket.withFilesForApproval(TicketTestUtils.getFilesForApproval(publication)).approveFiles()
            .publishApprovedFiles(resourceService);
        return ticket;
    }

    private static List<Contributor> extractContributors(ExpandedResource expandedResource)
        throws JsonProcessingException {
        return objectMapper.readValue(expandedResource.asJsonNode().at("/entityDescription").toString(),
                                      EntityDescription.class).getContributors();
    }

    private static EntityDescription createEntityDescriptionWithContributor(Contributor contributor) {
        var entityDescription = new EntityDescription();
        entityDescription.setContributors(List.of(contributor));
        return entityDescription;
    }

    private TicketEntry persistPublishingRequestContainingExistingUnpublishedFiles(Publication publication)
        throws ApiGatewayException {
        return PublishingRequestCase.create(Resource.fromPublication(publication),
                                            UserInstance.fromPublication(publication),
                                            PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
                   .persistNewTicket(ticketService);
    }

    private static List<Contributor> extractContributorsWithId(URI id, Publication publication) {
        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(contributor -> nonNull(contributor.getIdentity()))
                   .filter(contributor -> nonNull(getId(contributor)))
                   .filter(contributor -> getId(contributor).equals(id))
                   .toList();
    }

    private static URI getId(Contributor contributor) {
        return contributor.getIdentity().getId();
    }

    private static URI constructExpectedPublicationId(Publication publication) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild("publication")
                   .addChild(publication.getIdentifier().toString())
                   .getUri();
    }

    private static List<Class<?>> listPublicationInstanceTypes() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes();
    }

    private static List<Arguments> licenseProvider() {
        return List.of(Arguments.of("https://creativecommons.org/licenses/by-nc-nd/2.5", LicenseType.CC_NC_ND),
                       Arguments.of("https://creativecommons.org/licenses/by-nc-sa/2.0", LicenseType.CC_NC_SA),
                       Arguments.of("https://creativecommons.org/licenses/by-nc/3.0", LicenseType.CC_NC),
                       Arguments.of("https://creativecommons.org/licenses/by-nd/4.0", LicenseType.CC_ND),
                       Arguments.of("https://creativecommons.org/licenses/by-sa/4.0", LicenseType.CC_SA),
                       Arguments.of("https://creativecommons.org/licenses/by/4.0", LicenseType.CC_BY),
                       Arguments.of("https://creativecommons.org/publicdomain/zero/1.0/", LicenseType.CC_ZERO),
                       Arguments.of("http://rightsstatements.org/vocab/InC/1.0/", LicenseType.COPYRIGHT_ACT),
                       Arguments.of("http://rightsstatements.org/vocab/inc/1.0/", LicenseType.COPYRIGHT_ACT),
                       Arguments.of("https://nva.sikt.no/license/copyright-act/1.0", LicenseType.COPYRIGHT_ACT),
                       Arguments.of("https://something.else.com", LicenseType.OTHER)
        );
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends TicketEntry> someOtherTicketTypeBesidesDoiRequest(
        Class<? extends TicketEntry> ticketType) {
        return (Class<? extends TicketEntry>) ticketTypeProvider().map(Named::getPayload).filter(
            type -> !ticketType.equals(type) && !type.equals(DoiRequest.class)).findAny().orElseThrow();
    }

    private static ExpandedPerson getExpectedExpandedPerson(User user) {
        return new ExpandedPerson("someFirstName", "somePreferredFirstName", "someLastName", "somePreferredLastName",
                                  user);
    }

    private static TicketStatus getTicketStatus(ExpandedTicketStatus expandedTicketStatus) {
        return ExpandedTicketStatus.NEW.equals(expandedTicketStatus) ? TicketStatus.PENDING
                   : TicketStatus.parse(expandedTicketStatus.toString());
    }

    private static Publication getSamplePublication() throws JsonProcessingException {
        var samplePublicationAsJsonString = stringFromResources(Path.of("publication_sample.json"));
        return objectMapper.readValue(samplePublicationAsJsonString, Publication.class);
    }

    private Publication getPublicationWithSamePersonInDifferentContributorRoles(URI id) throws JsonProcessingException {
        var publication = getSamplePublication();
        var contributors = new ArrayList<>(publication.getEntityDescription().getContributors());
        var name = randomString();
        contributors.add(createContributor(randomElement(Role.values()), id, name, List.of(randomOrganization()), 1));
        contributors.add(createContributor(randomElement(Role.values()), id, name, List.of(randomOrganization()), 2));
        publication.getEntityDescription().setContributors(contributors);
        return publication;
    }

    private Contributor createContributor(Role role,
                                          URI id,
                                          String name,
                                          List<Corporation> affiliations,
                                          int sequence) {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder()
                                     .withName(name)
                                     .withId(id)
                                     .build())
                   .withRole(new RoleType(role))
                   .withAffiliations(affiliations)
                   .withSequence(sequence)
                   .build();
    }

    private ExpandedMessage messageToExpandedMessage(Message message) {
        return ExpandedMessage.builder()
                   .withStatus(MessageStatus.ACTIVE)
                   .withCreatedDate(message.getCreatedDate())
                   .withModifiedDate(message.getModifiedDate())
                   .withOwner(message.getOwner())
                   .withResourceTitle(message.getResourceTitle())
                   .withCustomerId(message.getCustomerId())
                   .withSender(new ExpandedPerson.Builder().withFirstName("someFirstName")
                                   .withLastName("someLastName")
                                   .withUser(message.getSender())
                                   .withPreferredLastName("somePreferredLastName")
                                   .withPreferredFirstName("somePreferredFirstName")
                                   .build())
                   .withText(message.getText())
                   .withTicketIdentifier(message.getTicketIdentifier())
                   .withResourceIdentifier(message.getResourceIdentifier())
                   .withIdentifier(message.getIdentifier())
                   .build();
    }

    private Entity findEntity(String type) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(PUBLISHED, USER, resourceService);
        publication.setEntityDescription(new EntityDescription());
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);

        var ticket = TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);
        FakeUriResponse.setupFakeForType(ticket, fakeUriRetriever);

        return switch (type) {
            case "Resource" -> Resource.fromPublication(publication);
            case "TicketEntry" -> ticket;
            case "Message" -> messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
            default -> throw new IllegalArgumentException("Unknown Entity type");
        };
    }

    private TicketEntry ticketWithAssignee(Class<? extends TicketEntry> ticketType, Publication publication)
        throws ApiGatewayException {
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticketService.updateTicketAssignee(ticket, new Username(USER.getUsername()));
        return ticketService.fetchTicket(ticket);
    }

    private ResourceExpansionService mockedExpansionService() {
        return new ResourceExpansionServiceImpl(resourceService, ticketService, fakeUriRetriever, fakeUriRetriever,
                                                sqsClient);
    }

    @SuppressWarnings("SameParameterValue")
    //Currently only GeneralSupportCase supports multiple simultaneous entries.
    // This may change in the future, so the warning is suppressed.
    private List<ExpandedMessage> messagesOfDifferentTickets(Publication publication, UserInstance owner,
                                                             Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {

        var differentTicketSameType = TicketEntry.requestNewTicket(publication, ticketType)
                                          .withOwner(UserInstance.fromPublication(publication).getUsername())
                                          .persistNewTicket(ticketService);
        var firstUnexpectedMessage = ExpandedMessage.createEntry(
            messageService.createMessage(differentTicketSameType, owner, randomString()), expansionService);
        var differentTicketType = someOtherTicketTypeBesidesDoiRequest(ticketType);
        var differentTicketDifferentType = TicketEntry.requestNewTicket(publication, differentTicketType)
                                               .withOwner(UserInstance.fromPublication(publication).getUsername())
                                               .persistNewTicket(ticketService);
        var secondUnexpectedMessage = ExpandedMessage.createEntry(
            messageService.createMessage(differentTicketDifferentType, owner, randomString()), expansionService);
        return new ArrayList<>(List.of(firstUnexpectedMessage, secondUnexpectedMessage));
    }

    private void initializeServices() {
        resourceService = getResourceService(client);
        messageService = getMessageService();
        ticketService = getTicketService();
        fakeUriRetriever = FakeUriRetriever.newInstance();
        sqsClient = new FakeSqsClient();
        expansionService = new ResourceExpansionServiceImpl(resourceService,
                                                            ticketService,
                                                            fakeUriRetriever,
                                                            fakeUriRetriever, sqsClient);
    }

    private Publication persistDraftPublicationWithoutDoi() throws BadRequestException {
        var publication = randomDegreePublication().copy().withDoi(null).withStatus(DRAFT).build();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private TicketEntry toTicketEntry(ExpandedPublishingRequest expandedPublishingRequest) {
        var publishingRequest = new PublishingRequestCase();
        publishingRequest.setResourceIdentifier(expandedPublishingRequest.getPublication().getIdentifier());
        publishingRequest.setCustomerId(expandedPublishingRequest.getCustomerId());
        publishingRequest.setIdentifier(extractIdentifier(expandedPublishingRequest.getId()));
        publishingRequest.setOwner(expandedPublishingRequest.getOwner().username());
        publishingRequest.setModifiedDate(expandedPublishingRequest.getModifiedDate());
        publishingRequest.setCreatedDate(expandedPublishingRequest.getCreatedDate());
        publishingRequest.setStatus(getTicketStatus(expandedPublishingRequest.getStatus()));
        publishingRequest.setFinalizedBy(extractUsername(expandedPublishingRequest.getFinalizedBy()));
        publishingRequest.setAssignee(extractUsername(expandedPublishingRequest.getAssignee()));
        publishingRequest.setApprovedFiles(expandedPublishingRequest.getApprovedFiles());
        publishingRequest.setFilesForApproval(expandedPublishingRequest.getFilesForApproval());
        publishingRequest.setOwnerAffiliation(expandedPublishingRequest.getOrganization().id());
        publishingRequest.setWorkflow(expandedPublishingRequest.getWorkflow());
        return publishingRequest;
    }

    private Username extractUsername(ExpandedPerson expandedPerson) {
        return Optional.ofNullable(expandedPerson)
                   .map(ExpandedPerson::username)
                   .map(User::toString)
                   .map(Username::new)
                   .orElse(null);
    }
}
