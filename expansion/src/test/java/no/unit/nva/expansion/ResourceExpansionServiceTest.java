package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ResourceExpansionServiceTest extends ResourcesLocalTest {

    private static final Clock CLOCK = Clock.systemDefaultZone();
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String WORKFLOW = "workflow";
    private static final String ASSIGNEE = "assignee";
    private static final String FINALIZED_BY = "finalizedBy";

    private ResourceExpansionService expansionService;
    private ResourceService resourceService;
    private MessageService messageService;
    private TicketService ticketService;

    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
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

    @SuppressWarnings("unchecked")
    private static Class<? extends TicketEntry> someOtherTicketTypeBesidesDoiRequest(
            Class<? extends TicketEntry> ticketType) {
        return (Class<? extends TicketEntry>)
                ticketTypeProvider().filter(type -> !ticketType.equals(type) && !type.equals(DoiRequest.class))
                        .findAny().orElseThrow();
    }

    @BeforeEach
    void setUp() {
        super.init();
        initializeServices();
    }

    @Test
    void test() throws JsonProcessingException, NotFoundException {
//        var json = jsonString();
//        var publication = JsonUtils.dtoObjectMapper.readValue(json, Publication.class);

        Publication publication = PublicationGenerator.randomPublication(ChapterArticle.class);

        Resource resourceUpdate = Resource.fromPublication(publication);
        ExpandedResource indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        assertThat(indexDoc.fetchId(), is(not(nullValue())));

    }

    private String jsonString() {
        return "{\n"
               + "  \"type\": \"Publication\",\n"
               + "  \"identifier\": \"0185bf9e9852-afde87d0-778a-4e39-93bd-94384df095c0\",\n"
               + "  \"status\": \"PUBLISHED\",\n"
               + "  \"resourceOwner\": {\n"
               + "    \"owner\": \"1136806@20754.0.0.0\",\n"
               + "    \"ownerAffiliation\": \"https://api.test.nva.aws.unit.no/cristin/organization/20754.0.0.0\"\n"
               + "  },\n"
               + "  \"publisher\": {\n"
               + "    \"type\": \"Organization\",\n"
               + "    \"id\": \"https://api.test.nva.aws.unit.no/customer/0baf8fcb-b18d-4c09-88bb-956b4f659103\",\n"
               + "    \"labels\": {}\n"
               + "  },\n"
               + "  \"createdDate\": \"2023-01-17T12:05:20.082265Z\",\n"
               + "  \"modifiedDate\": \"2023-05-02T11:45:11.278112Z\",\n"
               + "  \"publishedDate\": \"2023-01-17T12:08:21.325235Z\",\n"
               + "  \"entityDescription\": {\n"
               + "    \"type\": \"EntityDescription\",\n"
               + "    \"mainTitle\": \"Agile development in non-agile organizations#\",\n"
               + "    \"alternativeTitles\": {},\n"
               + "    \"language\": \"http://lexvo.org/id/iso639-3/eng\",\n"
               + "    \"publicationDate\": {\n"
               + "      \"type\": \"PublicationDate\",\n"
               + "      \"year\": \"2023\",\n"
               + "      \"month\": \"1\",\n"
               + "      \"day\": \"10\"\n"
               + "    },\n"
               + "    \"contributors\": [\n"
               + "      {\n"
               + "        \"type\": \"Contributor\",\n"
               + "        \"identity\": {\n"
               + "          \"type\": \"Identity\",\n"
               + "          \"id\": \"https://api.test.nva.aws.unit.no/cristin/person/1136806\",\n"
               + "          \"name\": \"Ketil Aasarød\"\n"
               + "        },\n"
               + "        \"affiliations\": [\n"
               + "          {\n"
               + "            \"type\": \"Organization\",\n"
               + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/1615.0.0.0\",\n"
               + "            \"labels\": {}\n"
               + "          },\n"
               + "          {\n"
               + "            \"type\": \"Organization\",\n"
               + "            \"id\": \"https://api.test.nva.aws.unit.no/cristin/organization/20754.2.0.0\",\n"
               + "            \"labels\": {}\n"
               + "          }\n"
               + "        ],\n"
               + "        \"role\": {\n"
               + "          \"type\": \"Creator\"\n"
               + "        },\n"
               + "        \"sequence\": 1,\n"
               + "        \"correspondingAuthor\": false\n"
               + "      }\n"
               + "    ],\n"
               + "    \"alternativeAbstracts\": {},\n"
               + "    \"tags\": [\n"
               + "      \"agile\",\n"
               + "      \"software\"\n"
               + "    ],\n"
               + "    \"description\": \"Agile development in non-agile organizations\",\n"
               + "    \"reference\": {\n"
               + "      \"type\": \"Reference\",\n"
               + "      \"publicationContext\": {\n"
               + "        \"type\": \"Report\",\n"
               + "        \"series\": {\n"
               + "          \"type\": \"UnconfirmedSeries\"\n"
               + "        },\n"
               + "        \"publisher\": {\n"
               + "          \"type\": \"Publisher\",\n"
               + "          \"id\": \"https://api.test.nva.aws.unit.no/publication-channels/publisher/22839/2023\"\n"
               + "        },\n"
               + "        \"isbnList\": []\n"
               + "      },\n"
               + "      \"publicationInstance\": {\n"
               + "        \"type\": \"ReportResearch\",\n"
               + "        \"pages\": {\n"
               + "          \"type\": \"MonographPages\",\n"
               + "          \"illustrated\": false\n"
               + "        }\n"
               + "      }\n"
               + "    },\n"
               + "    \"abstract\": \"Agile development in non-agile organizations\"\n"
               + "  },\n"
               + "  \"projects\": [],\n"
               + "  \"fundings\": [],\n"
               + "  \"subjects\": [],\n"
               + "  \"associatedArtifacts\": [\n"
               + "    {\n"
               + "      \"type\": \"UnpublishedFile\",\n"
               + "      \"identifier\": \"49317279-6e6a-4d06-a7ab-9e724ebcd42b\",\n"
               + "      \"name\": \"agile.png\",\n"
               + "      \"mimeType\": \"image/png\",\n"
               + "      \"size\": 38399,\n"
               + "      \"license\": {\n"
               + "        \"type\": \"License\",\n"
               + "        \"identifier\": \"CC0\",\n"
               + "        \"labels\": {\n"
               + "          \"nb\": \"CC0\"\n"
               + "        }\n"
               + "      },\n"
               + "      \"administrativeAgreement\": false,\n"
               + "      \"publisherAuthority\": false,\n"
               + "      \"visibleForNonOwner\": false\n"
               + "    }\n"
               + "  ],\n"
               + "  \"additionalIdentifiers\": [],\n"
               + "  \"@context\": {\n"
               + "    \"@vocab\": \"https://nva.sikt.no/ontology/publication#\",\n"
               + "    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\",\n"
               + "    \"id\": \"@id\",\n"
               + "    \"type\": \"@type\",\n"
               + "    \"affiliations\": {\n"
               + "      \"@id\": \"affiliation\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"activeFrom\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"activeTo\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"associatedArtifacts\": {\n"
               + "      \"@id\": \"associatedArtifact\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"additionalIdentifiers\": {\n"
               + "      \"@id\": \"additionalIdentifier\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"alternativeTitles\": {\n"
               + "      \"@id\": \"alternativeTitle\",\n"
               + "      \"@container\": \"@language\"\n"
               + "    },\n"
               + "    \"approvals\": {\n"
               + "      \"@id\": \"approval\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"approvalStatus\": {\n"
               + "      \"@type\": \"@vocab\",\n"
               + "      \"@context\": {\n"
               + "        \"@vocab\": \"https://nva.sikt.no/ontology/publication#\"\n"
               + "      }\n"
               + "    },\n"
               + "    \"approvedBy\": {\n"
               + "      \"@type\": \"@vocab\",\n"
               + "      \"@context\": {\n"
               + "        \"@vocab\": \"https://nva.sikt.no/ontology/approvals-body#\"\n"
               + "      }\n"
               + "    },\n"
               + "    \"architectureOutput\": {\n"
               + "      \"@id\": \"architectureOutput\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"compliesWith\": {\n"
               + "      \"@id\": \"compliesWith\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"concertProgramme\": {\n"
               + "      \"@id\": \"concertProgramme\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"contributors\": {\n"
               + "      \"@id\": \"contributor\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"createdDate\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"date\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"doi\": {\n"
               + "      \"@type\": \"@id\"\n"
               + "    },\n"
               + "    \"embargoDate\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"from\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"handle\": {\n"
               + "      \"@type\": \"@id\"\n"
               + "    },\n"
               + "    \"indexedDate\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"isbnList\": {\n"
               + "      \"@id\": \"isbn\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"labels\": {\n"
               + "      \"@id\": \"label\",\n"
               + "      \"@container\": \"@language\"\n"
               + "    },\n"
               + "    \"language\": {\n"
               + "      \"@type\": \"@id\"\n"
               + "    },\n"
               + "    \"link\": {\n"
               + "      \"@type\": \"@id\"\n"
               + "    },\n"
               + "    \"manifestations\": {\n"
               + "      \"@id\": \"manifestation\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"metadataSource\": {\n"
               + "      \"@type\": \"@id\"\n"
               + "    },\n"
               + "    \"modifiedDate\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"musicalWorks\": {\n"
               + "      \"@id\": \"musicalWork\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"ownerAffiliation\": {\n"
               + "      \"@type\": \"@id\"\n"
               + "    },\n"
               + "    \"outputs\": {\n"
               + "      \"@id\": \"output\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"publishedDate\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"nameType\": {\n"
               + "      \"@type\": \"@vocab\",\n"
               + "      \"@context\": {\n"
               + "        \"@vocab\": \"https://nva.sikt.no/ontology/publication#\"\n"
               + "      }\n"
               + "    },\n"
               + "    \"projects\": {\n"
               + "      \"@id\": \"project\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"fundings\": {\n"
               + "      \"@id\": \"funding\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"related\": {\n"
               + "      \"@id\": \"related\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"referencedBy\": {\n"
               + "      \"@id\": \"referencedBy\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"role\": {\n"
               + "      \"@type\": \"@vocab\",\n"
               + "      \"@context\": {\n"
               + "        \"@vocab\": \"https://nva.sikt.no/ontology/publication#\"\n"
               + "      }\n"
               + "    },\n"
               + "    \"status\": {\n"
               + "      \"@type\": \"@vocab\",\n"
               + "      \"@context\": {\n"
               + "        \"@vocab\": \"https://nva.sikt.no/ontology/publication#\"\n"
               + "      }\n"
               + "    },\n"
               + "    \"subjects\": {\n"
               + "      \"@id\": \"subject\",\n"
               + "      \"@type\": \"@id\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"tags\": {\n"
               + "      \"@id\": \"tag\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"to\": {\n"
               + "      \"@type\": \"xsd:dateTime\"\n"
               + "    },\n"
               + "    \"trackList\": {\n"
               + "      \"@id\": \"trackList\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    },\n"
               + "    \"venues\": {\n"
               + "      \"@id\": \"venue\",\n"
               + "      \"@container\": \"@set\"\n"
               + "    }\n"
               + "  },\n"
               + "  \"id\": \"https://api.test.nva.aws.unit"
               + ".no/publication/0185bf9e9852-afde87d0-778a-4e39-93bd-94384df095c0\"\n"
               + "}";
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingTheOrganizationIdOfTheOwnersAffiliationAsIs(
            Class<? extends TicketEntry> ticketType,
            PublicationStatus status)
            throws Exception {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var userAffiliation = publication.getResourceOwner().getOwnerAffiliation();
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(userAffiliation, is(in(expandedTicket.getOrganizationIds())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingFinalizedByValue(
            Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException,
            JsonProcessingException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setFinalizedBy(new Username(randomString()));
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(ticket.getFinalizedBy(), is(equalTo(expandedTicket.getFinalizedBy())));
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
        var regeneratedTicket = expandedTicket.toTicketEntry();

        assertThat(regeneratedTicket, is(equalTo(ticket)));
        assertThat(ticket, doesNotHaveEmptyValuesIgnoringFields(Set.of(WORKFLOW, ASSIGNEE, FINALIZED_BY,
                FINALIZED_DATE)));
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
        assertThat(messages, contains(message));
    }

    @ParameterizedTest(name = "should return framed index document for resources. Instance type:{0}")
    @MethodSource("listPublicationInstanceTypes")
    void shouldReturnFramedIndexDocumentFromResource(Class<?> instanceType)
            throws JsonProcessingException, NotFoundException {

        Publication publication = PublicationGenerator.randomPublication(instanceType)
                .copy().withEntityDescription(new EntityDescription()).build();

        Resource resourceUpdate = Resource.fromPublication(publication);
        ExpandedResource indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        assertThat(indexDoc.fetchId(), is(not(nullValue())));
    }

    @Test
    void shouldIncludedOnlyMessagesAssociatedToExpandedTicket() throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var owner = UserInstance.fromPublication(publication);

        var ticketToBeExpanded = TicketEntry
                .requestNewTicket(publication, GeneralSupportRequest.class)
                .persistNewTicket(ticketService);

        var expectedMessage = messageService.createMessage(ticketToBeExpanded, owner, randomString());

        var unexpectedMessages = messagesOfDifferentTickets(publication, owner, GeneralSupportRequest.class);
        var expandedEntry = (ExpandedTicket) expansionService.expandEntry(ticketToBeExpanded);
        assertThat(expandedEntry.getMessages(), contains(expectedMessage));
        assertThat(unexpectedMessages, everyItem(not(in(expandedEntry.getMessages()))));
    }

    @Test
    void shouldExpandAssociatedTicketAndNotTheMessageItselfWhenNewMessageArrivesForExpansion()
            throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var owner = UserInstance.fromPublication(publication);

        var ticketToBeExpanded = TicketEntry
                .requestNewTicket(publication, GeneralSupportRequest.class)
                .persistNewTicket(ticketService);

        var messageThatWillLeadToTicketExpansion =
                messageService.createMessage(ticketToBeExpanded, owner, randomString());

        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(messageThatWillLeadToTicketExpansion);
        assertThat(expandedTicket.getMessages(), contains(messageThatWillLeadToTicketExpansion));
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
    void shouldGetAllOrganizationIdsForAffiliations(Class<? extends TicketEntry> ticketType, PublicationStatus status)
            throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

        var expectedOrgIds = Set.of(publication.getResourceOwner().getOwnerAffiliation());
        var orgIds = expansionService.getOrganizationIds(ticket);

        assertThat(orgIds, is(equalTo(expectedOrgIds)));
    }

    @Test
    void shouldReturnEmptySetIfNotTicketEntry() throws NotFoundException {
        var message = Message.builder()
                .withResourceIdentifier(SortableIdentifier.next())
                .withTicketIdentifier(SortableIdentifier.next())
                .withIdentifier(SortableIdentifier.next())
                .build();

        var actual = expansionService.getOrganizationIds(message);

        assertThat(actual, is(equalTo(Collections.emptySet())));
    }

    @SuppressWarnings("SameParameterValue")
    //Currently only GeneralSupportCase supports multiple simultaneous entries.
    // This may change in the future, so the warning is suppressed.
    private List<Message> messagesOfDifferentTickets(Publication publication, UserInstance owner,
                                                     Class<? extends TicketEntry> ticketType)
            throws ApiGatewayException {

        var differentTicketSameType = TicketEntry.requestNewTicket(publication, ticketType)
                .persistNewTicket(ticketService);
        var firstUnexpectedMessage = messageService.createMessage(differentTicketSameType, owner, randomString());
        var differentTicketType = someOtherTicketTypeBesidesDoiRequest(ticketType);
        var differentTicketDifferentType =
                TicketEntry.requestNewTicket(publication, differentTicketType).persistNewTicket(ticketService);
        var secondUnexpectedMessage = messageService.createMessage(differentTicketDifferentType, owner, randomString());
        return new ArrayList<>(List.of(firstUnexpectedMessage, secondUnexpectedMessage));
    }

    private void initializeServices() {
        resourceService = new ResourceService(client, CLOCK);
        messageService = new MessageService(client);
        ticketService = new TicketService(client);
        UriRetriever uriRetriever = mock(UriRetriever.class);
        expansionService = new ResourceExpansionServiceImpl(resourceService, ticketService, uriRetriever);
    }

    private Publication persistDraftPublicationWithoutDoi() throws BadRequestException {
        var publication =
                randomPublication().copy()
                        .withDoi(null)
                        .withStatus(DRAFT)
                        .build();
        return Resource.fromPublication(publication).persistNew(resourceService,
                UserInstance.fromPublication(publication));
    }
}
