package no.unit.nva.expansion;

import static java.util.Objects.nonNull;
import static no.unit.nva.expansion.model.ExpandedTicket.extractIdentifier;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedGeneralSupportRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedOrganization;
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
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileForApproval;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceExpansionServiceTest extends ResourcesLocalTest {

    public static final URI ORGANIZATION =
        URI.create("https://api.dev.nva.aws.unit.no/cristin/person/myCristinId/myOrganization");
    public static final UserInstance USER = UserInstance.create(new User("12345"), ORGANIZATION);
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String WORKFLOW = "workflow";
    private static final String ASSIGNEE = "assignee";
    private static final String OWNERAFFILIATION = "ownerAffiliation";
    private static final String FINALIZED_BY = "finalizedBy";
    public static final String APPROVED_FILES = "approvedFiles";
    public static final String FILES_FOR_APPROVAL = "filesForApproval";
    private ResourceExpansionService expansionService;
    private ResourceService resourceService;
    private MessageService messageService;
    private TicketService ticketService;
    private UriRetriever personRetriever;
    private UriRetriever orgRetriever;

    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }

    @BeforeEach
    void setUp() {
        super.init();
        initializeServices();
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingTheOrganizationOfTheOwnersAffiliationAsIs(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws Exception {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setOwnerAffiliation(randomUri());
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(expandedTicket.getOrganization().id(), is(equalTo(ticket.getOwnerAffiliation())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingFinalizedByValue(Class<? extends TicketEntry> ticketType,
                                                              PublicationStatus status) throws ApiGatewayException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setFinalizedBy(new Username(randomString()));

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
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        var regeneratedTicket = toTicketEntry(expandedTicket);

        assertThat(regeneratedTicket, is(equalTo(ticket)));
        assertThat(ticket,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(WORKFLOW, ASSIGNEE, FINALIZED_BY,
                                                               FINALIZED_DATE, OWNERAFFILIATION, APPROVED_FILES,
                                                               FILES_FOR_APPROVAL)));
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
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        var messages = expandedTicket.getMessages();
        var expectedExpandedMessage = messageToExpandedMessage(message);
        assertThat(messages, contains(expectedExpandedMessage));
    }

    @ParameterizedTest(name = "should return framed index document for resources. Instance type:{0}")
    @MethodSource("listPublicationInstanceTypes")
    void shouldReturnFramedIndexDocumentFromResource(Class<?> instanceType)
        throws JsonProcessingException, NotFoundException {

        Publication publication = PublicationGenerator.randomPublication(instanceType)
                                      .copy()
                                      .withEntityDescription(new EntityDescription())
                                      .build();

        Resource resourceUpdate = Resource.fromPublication(publication);
        ExpandedResource indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        assertThat(indexDoc.fetchId(), is(not(nullValue())));
    }

    @ParameterizedTest(name = "should return framed index document containing license:{0}")
    @MethodSource("licenseProvider")
    void shouldReturnIndexDocumentContainingLicense(String licenseUri, LicenseType expectedLicense)
        throws JsonProcessingException, NotFoundException {
        var fileWithLicense = randomPublishedFile(licenseUri);
        var associatedLink = new AssociatedLink(randomUri(), null, null);
        var publication = PublicationGenerator.randomPublication()
                                      .copy()
                                      .withAssociatedArtifacts(List.of(fileWithLicense, associatedLink))
                                      .build();

        var resourceUpdate = Resource.fromPublication(publication);
        var indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var licensesAsString = getLicenseForFile(indexDoc);
        var license = JsonUtils.dtoObjectMapper.readValue(licensesAsString, License.class);

        assertThat(license.name(), is(equalTo(expectedLicense.toLicense(URI.create(licenseUri)).name())));
    }

    @Test
    void shouldReturnIndexDocumentWithoutLicenseWhenNoLicense() throws JsonProcessingException, NotFoundException {
            var fileWithLicense = File.builder().buildPublishedFile();
            var publication = PublicationGenerator.randomPublication()
                                  .copy()
                                  .withAssociatedArtifacts(List.of(fileWithLicense))
                                  .build();

            var resourceUpdate = Resource.fromPublication(publication);
            var indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
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

    private File randomPublishedFile(String license) {
        return File.builder()
                   .withName(randomString())
                   .withLicense(URI.create(license))
                   .withIdentifier(UUID.randomUUID())
                   .buildPublishedFile();
    }

    @Test
    void shouldReturnIndexDocumentWithContextUri()
        throws JsonProcessingException, NotFoundException {

        var publication = randomPublication(AcademicArticle.class)
                                      .copy()
                                      .withEntityDescription(new EntityDescription())
                                      .build();

        var resourceUpdate = Resource.fromPublication(publication);
        var indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        assertThat(indexDoc.getAllFields().get("@context"),
                   is(equalTo("https://api.dev.nva.aws.unit.no/publication/context")));
    }

    @Test
    void shouldIncludedOnlyMessagesAssociatedToExpandedTicket() throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var owner = UserInstance.fromPublication(publication);

        var ticketToBeExpanded = TicketEntry.requestNewTicket(publication, GeneralSupportRequest.class)
                                     .persistNewTicket(ticketService);

        var message = messageService.createMessage(ticketToBeExpanded, owner, randomString());
        var expectedExpandedMessage = messageToExpandedMessage(message);
        var unexpectedMessages = messagesOfDifferentTickets(publication, owner, GeneralSupportRequest.class);
        var expandedEntry = (ExpandedTicket) expansionService.expandEntry(ticketToBeExpanded);
        assertThat(expandedEntry.getMessages(), hasItem(expectedExpandedMessage));
        assertThat(unexpectedMessages, everyItem(not(in(expandedEntry.getMessages()))));
    }

    @Test
    void shouldExpandAssociatedTicketAndNotTheMessageItselfWhenNewMessageArrivesForExpansion()
        throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var owner = UserInstance.fromPublication(publication);

        var ticketToBeExpanded = TicketEntry.requestNewTicket(publication, GeneralSupportRequest.class)
                                     .persistNewTicket(ticketService);

        var messageThatWillLeadToTicketExpansion = messageService.createMessage(ticketToBeExpanded, owner,
                                                                                randomString());
        var expectedExpandedMessage = messageToExpandedMessage(messageThatWillLeadToTicketExpansion);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(messageThatWillLeadToTicketExpansion);
        assertThat(expandedTicket.getMessages(), hasItem(expectedExpandedMessage));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldAddResourceTitleToExpandedTicket(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);

        var expectedTitle = publication.getEntityDescription().getMainTitle();
        assertThat(expandedTicket.getPublication().getTitle(), is(equalTo(expectedTitle)));
    }

    @Test
    void shouldThrowIfUnsupportedType() {
        var unsupportedImplementation = mock(Entity.class);
        assertThrows(UnsupportedOperationException.class,
                     () -> expansionService.expandEntry(unsupportedImplementation));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUseOwnerAffiliationWhenTicketHasOwnerAffiliation(Class<? extends TicketEntry> ticketType,
                                                                PublicationStatus status)
        throws Exception {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setOwnerAffiliation(randomUri());
        var expectedOrgId = ticket.getOwnerAffiliation();
        var actualAffiliation  = expansionService.getOrganization(ticket).id();
        assertThat(actualAffiliation, is(equalTo(expectedOrgId)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUseResourceOwnerAffiliationWhenTicketHasNoOwnerAffiliation(Class<? extends TicketEntry> ticketType,
                                                                          PublicationStatus status)
        throws Exception {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, USER, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expectedOrgId = publication.getResourceOwner().getOwnerAffiliation();
        var actualAffiliation  = expansionService.getOrganization(ticket).id();
        assertThat(actualAffiliation, is(equalTo(expectedOrgId)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldGetOrganizationPartOfsForAffiliations(Class<? extends TicketEntry> ticketType,
                                                       PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

        var expectedPartOf = List.of(
            new ExpandedOrganization(
                URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0"),
                "20754.0.0.0",
                null)
        );
        var partOf = expansionService.getOrganization(ticket).partOf();

        assertThat(partOf, is(equalTo(expectedPartOf)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldGetOrganizationIdentifierForAffiliations(Class<? extends TicketEntry> ticketType,
                                                     PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

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
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

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
        var ticket = ticketWithAssignee(ticketType, publication);

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
        expansionService.expandEntry(entity);

        assertThat(logAppender.getMessages(), containsString(type + ": " + entity.getIdentifier().toString()));
    }

    @Test
    void some() throws JsonProcessingException, NotFoundException {
        var p = """
            {
  "type": "Publication",
  "identifier": "018f3035b287-fc073a5c-374d-4494-9060-085a30883b3e",
  "status": "PUBLISHED",
  "resourceOwner": {
    "owner": "ntnu@194.0.0.0",
    "ownerAffiliation": "https://api.test.nva.aws.unit.no/cristin/organization/194.0.0.0"
  },
  "publisher": {
    "type": "Organization",
    "id": "https://api.test.nva.aws.unit.no/customer/0baf8fcb-b18d-4c09-88bb-956b4f659103"
  },
  "createdDate": "2020-12-15T00:00:00Z",
  "modifiedDate": "2024-05-07T17:30:54.416279201Z",
  "publishedDate": "2020-12-15T00:00:00Z",
  "entityDescription": {
    "type": "EntityDescription",
    "mainTitle": "NIKT: Norsk IKT-konferanse for forskning og utdanning 2020",
    "alternativeTitles": {},
    "language": "http://lexvo.org/id/iso639-3/und",
    "publicationDate": {
      "type": "PublicationDate",
      "year": "2020"
    },
    "contributors": [
      {
        "type": "Contributor",
        "identity": {
          "type": "Identity",
          "id": "https://api.test.nva.aws.unit.no/cristin/person/26638",
          "name": "Rune Hjelsvold",
          "verificationStatus": "Verified",
          "additionalIdentifiers": []
        },
        "affiliations": [
          {
            "type": "Organization",
            "id": "https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0"
          }
        ],
        "role": {
          "type": "EditorialBoardMember"
        },
        "sequence": 1,
        "correspondingAuthor": false
      },
      {
        "type": "Contributor",
        "identity": {
          "type": "Identity",
          "id": "https://api.test.nva.aws.unit.no/cristin/person/3549",
          "name": "Eric Bartley Jul",
          "verificationStatus": "Verified",
          "additionalIdentifiers": []
        },
        "affiliations": [
          {
            "type": "Organization",
            "id": "https://api.test.nva.aws.unit.no/cristin/organization/185.15.5.25"
          }
        ],
        "role": {
          "type": "EditorialBoardMember"
        },
        "sequence": 2,
        "correspondingAuthor": false
      }
    ],
    "alternativeAbstracts": {},
    "npiSubjectHeading": "1152",
    "tags": [],
    "reference": {
      "type": "Reference",
      "publicationContext": {
        "type": "Book",
        "series": {
          "type": "Series",
          "id": "https://api.test.nva.aws.unit.no/publication-channels-v2/journal/23E19F1D-8FAC-46E5-88A7-C628F3985910/2020"
        },
        "seriesNumber": "Issue:1",
        "publisher": {
          "type": "UnconfirmedPublisher",
          "name": "Bibsys Open Journal Systems",
          "valid": true
        },
        "isbnList": [
          "9783540681649"
        ],
        "revision": "Unrevised",
        "additionalIdentifiers": []
      },
      "publicationInstance": {
        "type": "BookAnthology",
        "pages": {
          "type": "MonographPages",
          "pages": "146",
          "illustrated": false
        }
      }
    }
  },
  "projects": [],
  "fundings": [],
  "subjects": [],
  "associatedArtifacts": [],
  "additionalIdentifiers": [
    {
      "type": "AdditionalIdentifier",
      "sourceName": "Cristin",
      "value": "1859916"
    }
  ],
  "allowedOperations": [
    "update",
    "unpublish"
  ],
  "publicationNotes": [],
  "id": "https://api.test.nva.aws.unit.no/publication/018f3035b287-fc073a5c-374d-4494-9060-085a30883b3e"
}
            """;
        var publication = JsonUtils.dtoObjectMapper.readValue(p, Publication.class);
        var s = expansionService.expandEntry(Resource.fromPublication(publication));
        var l = "";
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusNewWhenTicketStatusIsPendingWithoutAssignee(
        Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.NEW)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusClosedWhenTicketStatusIsClosed(Class<? extends TicketEntry> ticketType,
                                                                        PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setStatus(TicketStatus.CLOSED);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.CLOSED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusClosedWhenTicketStatusIsCompleted(Class<? extends TicketEntry> ticketType,
                                                                           PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setStatus(TicketStatus.COMPLETED);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.COMPLETED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingViewedByValue(Class<? extends TicketEntry> ticketType,
                                                           PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var viewedBySet = Set.of(new User(randomString()));
        ticket.setViewedBy(viewedBySet);

        expansionService = mockedExpansionService();

        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        var viewedBy = ticket.getViewedBy();
        var expectedExpandedViewedBy = getExpectedExpandedPerson(new User(viewedBy.toString()));
        assert expandedTicket.getViewedBy().contains(expectedExpandedViewedBy);
        var expectedExpandedViewedBySet = Set.of(expectedExpandedViewedBy);
        assertThat(expandedTicket.getViewedBy(), is(equalTo(expectedExpandedViewedBySet)));
    }

    @Test
    void shouldReturnExpandedResourceWithEntityDescriptionForPublicationWithContributorWithoutId()
        throws JsonProcessingException, NotFoundException {
        var publicationJsonString = stringFromResources(Path.of("publication_without_contributor_id_sample.json"));

        var publication = objectMapper.readValue(publicationJsonString, Publication.class);
        var resourceUpdate = Resource.fromPublication(publication);

        expansionService = mockedExpansionService();
        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualEntityDescription = expandedResource.asJsonNode().at("/entityDescription").toString();

        assertThat(actualEntityDescription, is(not(equalTo(""))));
    }

    @Test
    void shouldReturnExpandedResourceWithContributorForPublicationWithContributorWithoutIdOrAffiliation()
        throws JsonProcessingException, NotFoundException {
        var contributorWithoutIdOrAffiliation = createContributor(randomElement(Role.values()), null, randomString(),
                                                                  Collections.emptyList(), 1);
        var entityDescription = createEntityDescriptionWithContributor(contributorWithoutIdOrAffiliation);
        var publication = PublicationGenerator.randomPublication()
                              .copy()
                              .withEntityDescription(entityDescription)
                              .build();
        var resourceUpdate = Resource.fromPublication(publication);

        expansionService = mockedExpansionService();
        var expandedResource = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        var actualContributors = extractContributors(expandedResource);
        assertThat(actualContributors, containsInAnyOrder(List.of(contributorWithoutIdOrAffiliation).toArray()));
    }

    @Test
    void shouldReturnExpandedResourceWithCorrectNumberOfContributorsForPublicationWithSamePersonInDifferentRoles()
        throws JsonProcessingException, NotFoundException {
        var id = randomUri();
        var publication = getPublicationWithSamePersonInDifferentContributorRoles(id);
        var resourceUpdate = Resource.fromPublication(publication);

        expansionService = mockedExpansionService();
        var expandedResourceAsJson = expansionService.expandEntry(resourceUpdate).toJsonString();

        var regeneratedPublication = objectMapper.readValue(expandedResourceAsJson, Publication.class);

        var contributorsWithSameId = extractContributorsWithId(id, regeneratedPublication);
        assertThat(contributorsWithSameId.size(), is(equalTo(2)));
    }

    @Test
    void shouldExpandApprovedFilesForPublishingRequest()
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublicationWithUnpublishedFiles(PUBLISHED, resourceService);
        var ticket = createCompletedTicketAndPublishFiles(publication);
        var expandedTicket = (ExpandedPublishingRequest) expansionService.expandEntry(ticket);
        var regeneratedTicket = (PublishingRequestCase) toTicketEntry(expandedTicket);

        assertThat(regeneratedTicket, is(equalTo(ticket)));

        var publishedFilesFromPublication = resourceService.getPublication(publication)
                                                .getAssociatedArtifacts().stream()
                                                .filter(PublishedFile.class::isInstance)
                                                .collect(Collectors.toSet());
        var publishedFilesFromExpandedPublishingRequest = expandedTicket.getApprovedFiles();

        assertThat(publishedFilesFromPublication, 
                   containsInAnyOrder(publishedFilesFromExpandedPublishingRequest.toArray()));
    }

    @Test
    void shouldExpandFilesForApprovalForPublishingRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublicationWithUnpublishedFiles(PUBLISHED, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);
        var expandedTicket = (ExpandedPublishingRequest) expansionService.expandEntry(ticket);
        var regeneratedTicket = (PublishingRequestCase) toTicketEntry(expandedTicket);

        assertThat(regeneratedTicket, is(equalTo(ticket)));

        var expectedFilesForApproval = resourceService.getPublication(publication)
                                                .getAssociatedArtifacts().stream()
                                                .filter(UnpublishedFile.class::isInstance)
                                                .toArray();
        var filesForApproval = expandedTicket.getFilesForApproval();

        assertThat(filesForApproval, containsInAnyOrder(expectedFilesForApproval));
    }

    @Test
    void shouldExpandPublicationWithNviStatusWhenPublicationIsReportedNviCandidate()
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var resourceUpdate = Resource.fromPublication(publication);
        var expansionService = expansionServiceReturningNviCandidate(publication, nviCandidateResponse(), 200);
        var expandedResourceAsJson = expansionService.expandEntry(resourceUpdate).toJsonString();
        var json = JsonUtils.dtoObjectMapper.readTree(expandedResourceAsJson);
        var nviStatusNode = json.get(ScientificIndex.SCIENTIFIC_INDEX_FIELD);

        assertThat(nviStatusNode.get("year").asText(), is(equalTo("2024")));
        assertThat(nviStatusNode.get("status").asText(), is(equalTo("Reported")));
    }

    @Test
    void shouldNotAddNviStatusToPublicationWhenNotFoundResponseFromNvi()
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var resourceUpdate = Resource.fromPublication(publication);
        var expansionService = expansionServiceReturningNviCandidate(publication, nviCandidateResponse(), 404);
        var expandedResourceAsJson = expansionService.expandEntry(resourceUpdate).toJsonString();
        var json = JsonUtils.dtoObjectMapper.readTree(expandedResourceAsJson);

        assertThat(json.get("nviStatus"), is(nullValue()));
    }

    @Test
    void doiShouldBeExpandedAsUriWhenMultipleDoiOccurrencesInPublication()
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublicationWithDoi(PUBLISHED, resourceService);
        var doi = publication.getEntityDescription().getReference().getDoi();
        publication.getAssociatedArtifacts().add(new AssociatedLink(doi, randomString(), randomString()));
        var publicationWithIdenticalDoiValues = resourceService.updatePublication(publication);
        var resourceUpdate = Resource.fromPublication(publicationWithIdenticalDoiValues);
        var expansionService = expansionServiceReturningNviCandidate(publication, nviCandidateResponse(), 404);
        var expandedResourceAsJson = expansionService.expandEntry(resourceUpdate).toJsonString();
        var json = JsonUtils.dtoObjectMapper.readTree(expandedResourceAsJson);

        var expandedDoi = json.at("/entityDescription/reference/doi");
        assertThat(expandedDoi.asText(), is(equalTo(doi.toString())));
    }

    @Test
    void shouldThrowExpansionExceptionWhenUnexpectedErrorFetchingNviCandidateForPublication()
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var resourceUpdate = Resource.fromPublication(publication);
        var expansionService = expansionServiceReturningNviCandidate(publication, randomString(), 200);

        assertThrows(ExpansionException.class, () -> expansionService.expandEntry(resourceUpdate));
    }

    private ResourceExpansionServiceImpl expansionServiceReturningNviCandidate(Publication publication,
                                                                               String responseBody, int statusCode) {
        var mockUriRetriver = mock(UriRetriever.class);
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(responseBody);
        when(mockUriRetriver.fetchResponse(fetchNviCandidateUri(publication), "application/json")).thenReturn(Optional.of(response));
        return new ResourceExpansionServiceImpl(getResourceServiceBuilder().build(),
                                                            new TicketService(client, mockUriRetriver),
                                                            mockUriRetriver,
                                                            mockUriRetriver);
    }

    private String nviCandidateResponse() {
        return """
            {
             "type": "NviCandidate",
             "status": "Reported",
             "period": {
                    "type": "NviReportingPeriod",
                    "id": "https://api.sandbox.nva.aws.unit.no/scientific-index/period/2024",
                    "year": "2024"
                }
           }
           """;
    }

    private URI fetchNviCandidateUri(Publication publication) {
        var uri = UriWrapper.fromHost(API_HOST)
                   .addChild("scientific-index")
                   .addChild("candidate")
                   .addChild("publication")
                   .getUri();
        return URI.create(uri + "/" + URLEncoder.encode(constructExpectedPublicationId(publication).toString(),
                                                        StandardCharsets.UTF_8));
    }

    private TicketEntry createCompletedTicketAndPublishFiles(Publication publication) throws ApiGatewayException {
        var ticket = TicketTestUtils.createCompletedTicket(publication, PublishingRequestCase.class, ticketService);
        publishFiles(publication);
        return ticket;
    }

    private void publishFiles(Publication publication) {
        var updatedPublication = publication.copy()
                                     .withAssociatedArtifacts(convertUnpublishedFilesToPublished(publication))
                                     .build();
        resourceService.updatePublication(updatedPublication);
    }

    private static List<AssociatedArtifact> convertUnpublishedFilesToPublished(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(UnpublishedFile.class::isInstance)
                   .map(UnpublishedFile.class::cast)
                   .map(UnpublishedFile::toPublishedFile)
                   .collect(Collectors.toList());
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

    private static List<Contributor> extractContributorsWithId(URI id, Publication publication) {
        return publication.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(contributor -> nonNull(contributor.getIdentity()))
                   .filter(contributor -> nonNull(getId(contributor)))
                   .filter(contributor -> getId(contributor).equals(id))
                   .collect(Collectors.toList());
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
                       Arguments.of("http://rightsstatements.org/vocab/InC/1.0/", LicenseType.RS_INC),
                       Arguments.of("http://rightsstatements.org/vocab/inc/1.0/", LicenseType.RS_INC),
                       Arguments.of("https://something.else.com", LicenseType.OTHER)
        );
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends TicketEntry> someOtherTicketTypeBesidesDoiRequest(
        Class<? extends TicketEntry> ticketType) {
        return (Class<? extends TicketEntry>) ticketTypeProvider().filter(
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
                   .withSender(ExpandedPerson.defaultExpandedPerson(message.getSender()))
                   .withText(message.getText())
                   .withTicketIdentifier(message.getTicketIdentifier())
                   .withResourceIdentifier(message.getResourceIdentifier())
                   .withIdentifier(message.getIdentifier())
                   .build();
    }

    private Entity findEntity(String type) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(PUBLISHED, USER, resourceService);
        publication.setEntityDescription(new EntityDescription());
        var ticket = TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);

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
        personRetriever = mock(UriRetriever.class);
        orgRetriever = mock(UriRetriever.class);
        when(personRetriever.getRawContent(any(), any())).thenReturn(
            Optional.of(stringFromResources(Path.of("cristin_person.json"))));
        when(orgRetriever.getRawContent(any(), any())).thenReturn(
            Optional.of(stringFromResources(Path.of("organizations/20754.6.0.0.json"))));
        return new ResourceExpansionServiceImpl(resourceService, ticketService, personRetriever, orgRetriever);
    }

    @SuppressWarnings("SameParameterValue")
    //Currently only GeneralSupportCase supports multiple simultaneous entries.
    // This may change in the future, so the warning is suppressed.
    private List<ExpandedMessage> messagesOfDifferentTickets(Publication publication, UserInstance owner,
                                                             Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {

        var differentTicketSameType = TicketEntry.requestNewTicket(publication, ticketType)
                                          .persistNewTicket(ticketService);
        var firstUnexpectedMessage = ExpandedMessage.createEntry(
            messageService.createMessage(differentTicketSameType, owner, randomString()), expansionService);
        var differentTicketType = someOtherTicketTypeBesidesDoiRequest(ticketType);
        var differentTicketDifferentType = TicketEntry.requestNewTicket(publication, differentTicketType)
                                               .persistNewTicket(ticketService);
        var secondUnexpectedMessage = ExpandedMessage.createEntry(
            messageService.createMessage(differentTicketDifferentType, owner, randomString()), expansionService);
        return new ArrayList<>(List.of(firstUnexpectedMessage, secondUnexpectedMessage));
    }

    private void initializeServices() {
        resourceService = getResourceServiceBuilder().build();
        messageService = getMessageService();
        ticketService = getTicketService();
        personRetriever = mock(UriRetriever.class);
        orgRetriever = mock(UriRetriever.class);
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(personRetriever.fetchResponse(any(), any())).thenReturn(Optional.of(response));
        expansionService = new ResourceExpansionServiceImpl(resourceService,
                                                            ticketService,
                                                            personRetriever,
                                                            orgRetriever);

        when(orgRetriever.getRawContent(any(), any())).thenReturn(
            Optional.of(stringFromResources(Path.of("organizations/20754.6.0.0.json"))));
    }

    private Publication persistDraftPublicationWithoutDoi() throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).withStatus(DRAFT).build();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private DoiRequest toTicketEntry(ExpandedDoiRequest expandedDoiRequest) {
        DoiRequest doiRequest = new DoiRequest();
        doiRequest.setCreatedDate(expandedDoiRequest.getCreatedDate());
        doiRequest.setIdentifier(expandedDoiRequest.identifyExpandedEntry());
        doiRequest.setCustomerId(expandedDoiRequest.getCustomerId());
        doiRequest.setModifiedDate(expandedDoiRequest.getModifiedDate());
        doiRequest.setOwner(expandedDoiRequest.getOwner().username());
        doiRequest.setResourceIdentifier(expandedDoiRequest.getPublication().getIdentifier());
        doiRequest.setResourceStatus(expandedDoiRequest.getPublication().getStatus());
        doiRequest.setStatus(getTicketStatus(expandedDoiRequest.getStatus()));
        doiRequest.setAssignee(extractUsername(expandedDoiRequest.getAssignee()));
        return doiRequest;
    }

    private GeneralSupportRequest toTicketEntry(ExpandedGeneralSupportRequest expandedGeneralSupportRequest) {
        var ticketEntry = new GeneralSupportRequest();
        ticketEntry.setModifiedDate(expandedGeneralSupportRequest.getModifiedDate());
        ticketEntry.setCreatedDate(expandedGeneralSupportRequest.getCreatedDate());
        ticketEntry.setCustomerId(expandedGeneralSupportRequest.getCustomerId());
        ticketEntry.setIdentifier(extractIdentifier(expandedGeneralSupportRequest.getId()));
        ticketEntry.setResourceIdentifier(expandedGeneralSupportRequest.getPublication().getIdentifier());
        ticketEntry.setStatus(getTicketStatus(expandedGeneralSupportRequest.getStatus()));
        ticketEntry.setOwner(expandedGeneralSupportRequest.getOwner().username());
        ticketEntry.setAssignee(extractUsername(expandedGeneralSupportRequest.getAssignee()));
        return ticketEntry;
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
        publishingRequest.setApprovedFiles(extractApprovedFiles(expandedPublishingRequest));
        publishingRequest.setFilesForApproval(extractFilesForApproval(expandedPublishingRequest));
        return publishingRequest;
    }

    private Set<FileForApproval> extractFilesForApproval(ExpandedPublishingRequest expandedPublishingRequest) {
        return expandedPublishingRequest.getFilesForApproval().stream()
                   .map(FileForApproval::fromFile)
                   .collect(Collectors.toSet());
    }

    private static Set<UUID> extractApprovedFiles(ExpandedPublishingRequest expandedPublishingRequest) {
        return expandedPublishingRequest.getApprovedFiles().stream().map(
            File::getIdentifier).collect(
            Collectors.toSet());
    }

    private UnpublishRequest toTicketEntry(ExpandedUnpublishRequest expandedUnpublishRequest) {
        var ticketEntry = new UnpublishRequest();
        ticketEntry.setModifiedDate(expandedUnpublishRequest.getModifiedDate());
        ticketEntry.setCreatedDate(expandedUnpublishRequest.getCreatedDate());
        ticketEntry.setCustomerId(expandedUnpublishRequest.getCustomerId());
        ticketEntry.setIdentifier(expandedUnpublishRequest.identifyExpandedEntry());
        ticketEntry.setResourceIdentifier(expandedUnpublishRequest.getPublication().getIdentifier());
        ticketEntry.setStatus(getTicketStatus(expandedUnpublishRequest.getStatus()));
        ticketEntry.setOwner(expandedUnpublishRequest.getOwner().username());
        ticketEntry.setAssignee(extractUsername(expandedUnpublishRequest.getAssignee()));
        return ticketEntry;
    }

    private TicketEntry toTicketEntry(ExpandedTicket expandedTicket) {
        if (expandedTicket instanceof ExpandedDoiRequest expandedDoiRequest) {
            return toTicketEntry(expandedDoiRequest);
        } else if (expandedTicket instanceof ExpandedPublishingRequest expandedPublishingRequest) {
            return toTicketEntry(expandedPublishingRequest);
        } else if (expandedTicket instanceof ExpandedGeneralSupportRequest expandedGeneralSupportRequest) {
            return toTicketEntry(expandedGeneralSupportRequest);
        } else if (expandedTicket instanceof ExpandedUnpublishRequest expandedUnpublishRequest) {
            return toTicketEntry(expandedUnpublishRequest);
        } else {
            return null;
        }
    }

    private Username extractUsername(ExpandedPerson expandedPerson) {
        return Optional.ofNullable(expandedPerson)
                   .map(ExpandedPerson::username)
                   .map(User::toString)
                   .map(Username::new)
                   .orElse(null);
    }
}
