package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.ID_JSON_PTR;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.JournalExpansionServiceImpl;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Revision;
import no.unit.nva.model.Username;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.MediaContributionPeriodical;
import no.unit.nva.model.contexttypes.NullPublisher;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate.Builder;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDataEntryTest extends ResourcesLocalTest {

    public static final String TYPE = "type";
    public static final String EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY = "Publication";

    private ResourceExpansionService resourceExpansionService;
    private ResourceService resourceService;
    private TicketService ticketService;
    private MessageService messageService;
    private UriRetriever uriRetriever;

    public static Stream<Class<?>> entryTypes() {
        return TypeProvider.listSubTypes(ExpandedDataEntry.class);
    }

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    public static Stream<PublicationContext> importCandidateContextTypeProvider()
        throws InvalidUnconfirmedSeriesException {
        return Stream.of(new Book(null, randomString(), new Publisher(randomUri()), List.of(), Revision.UNREVISED),
                         new Report(null, randomString(), null, null, List.of()),
                         new MediaContributionPeriodical(randomUri()));
    }

    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = getResourceServiceBuilder().build();
        this.messageService = getMessageService();
        this.ticketService = getTicketService();
        this.uriRetriever = mock(UriRetriever.class);
        when(uriRetriever.getRawContent(any(), any())).thenReturn(Optional.empty());

        this.resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, ticketService);
    }

    @ParameterizedTest()
    @MethodSource("importCandidateContextTypeProvider")
    void shouldExpandImportCandidateSuccessfully(PublicationContext publicationContext) {
        var importCandidate = randomImportCandidate(publicationContext);
        importCandidate.getEntityDescription().getContributors().stream()
            .map(Contributor::getAffiliations)
            .flatMap(List::stream)
            .filter(Organization.class::isInstance)
            .map(Organization.class::cast)
            .forEach(this::mockOrganizations);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        this.resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, ticketService);
    }

    @ParameterizedTest()
    @MethodSource("importCandidateContextTypeProvider")
    void shouldExpandImportCandidateSuccessfullyWhenBadResponseFromCustomerApi(PublicationContext publicationContext) {
        var importCandidate = randomImportCandidate(publicationContext);
        importCandidate.getEntityDescription().getContributors().stream()
            .map(Contributor::getAffiliations)
            .flatMap(List::stream)
            .filter(Organization.class::isInstance)
            .map(Organization.class::cast)
            .forEach(this::mockOrganizationCristinResponseOnly);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);

        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        this.resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, ticketService);
    }

    @Test
    void shouldExpandImportCandidateJournalSuccessfullyWhenBadResponseFromChannelRegistry() {
        final var logAppender = LogUtils.getTestingAppender(JournalExpansionServiceImpl.class);
        var journalId = randomUri();
        var journalContext = new Journal(journalId);
        mockBadRequestForChannelRegistry(journalId);
        var importCandidate = randomImportCandidate(journalContext);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getJournal(), is(equalTo(new ExpandedJournal(journalId, null))));
        assertThat(logAppender.getMessages(), containsString("Not Ok response from channel registry"));
    }

    @Test
    void shouldExpandJournalSuccessfullyWhenOkResponseFromChannelRegistry() throws JsonProcessingException {
        var journalId = randomUri();
        var journalContext = new Journal(journalId);
        var expectedJournalTitle = randomString();
        mockResponseForChannelRegistry(journalId, expectedJournalTitle);
        var importCandidate = randomImportCandidate(journalContext);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getJournal(),
                   is(equalTo(new ExpandedJournal(journalId, expectedJournalTitle))));
    }

    @Test
    void shouldExpandPublisherSuccessfullyWhenBadResponseFromChannelRegistry() {
        var publisherId = randomUri();
        var publisher = new Publisher(publisherId);
        var bookContext = new Book(null, null, publisher, null, null);
        mockBadRequestForChannelRegistry(publisherId);
        var importCandidate = randomImportCandidate(bookContext);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getPublisher(), is(equalTo(new ExpandedPublisher(publisherId, null))));
    }

    @Test
    void shouldExpandUnconfirmedPublisher() {
        var publisherName = randomString();
        var publisher = new UnconfirmedPublisher(publisherName);
        var bookContext = new Book(null, null, publisher, null, null);
        var importCandidate = randomImportCandidate(bookContext);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getPublisher(), is(equalTo(new ExpandedPublisher(null, publisherName))));
    }

    @Test
    void shouldSkipExpandingNullPublisher() {
        var publisher = new NullPublisher();
        var bookContext = new Book(null, null, publisher, null, null);
        var importCandidate = randomImportCandidate(bookContext);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));

        assertThat(expandedImportCandidate.getPublisher(), is((nullValue())));
    }

    @Test
    void shouldExpandPublisherSuccessfullyWhenOkResponseFromChannelRegistry() throws JsonProcessingException {
        var expectedPublisherTitle = randomString();
        var publisherId = randomUri();
        var publisher = new Publisher(publisherId);
        var bookContext = new Book(null, null, publisher, null, null);
        mockResponseForChannelRegistry(publisherId, expectedPublisherTitle);
        var importCandidate = randomImportCandidate(bookContext);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));

        assertThat(expandedImportCandidate.getPublisher(),
                   is((equalTo(new ExpandedPublisher(publisherId, expectedPublisherTitle)))));
    }

    @Test
    void shouldLogFailureToParseChannelRegistryResponse() {
        final var logAppender = LogUtils.getTestingAppender(JournalExpansionServiceImpl.class);
        var journalId = randomUri();
        var journalContext = new Journal(journalId);
        mockUnparsableResponseForChannelRegistry(journalId);
        var importCandidate = randomImportCandidate(journalContext);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getJournal(), is(equalTo(new ExpandedJournal(journalId, null))));
        assertThat(logAppender.getMessages(), containsString("Failed to parse channel registry response"));
    }

    @SuppressWarnings("unchecked")
    private void mockUnparsableResponseForChannelRegistry(URI journalId) {
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(randomString());
        when(uriRetriever.fetchResponse(eq(journalId), any())).thenReturn(Optional.of(response));
    }

    @SuppressWarnings("unchecked")
    private void mockResponseForChannelRegistry(URI journalId, String expectedJournalTitle)
        throws JsonProcessingException {
        var responseBody = new ChannelRegistryResponse(expectedJournalTitle);
        var responseBodyString = JsonUtils.dtoObjectMapper.writeValueAsString(responseBody);
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(responseBodyString);
        when(uriRetriever.fetchResponse(eq(journalId), any())).thenReturn(Optional.of(response));
    }

    @SuppressWarnings("unchecked")
    private void mockBadRequestForChannelRegistry(URI id) {
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(404);
        when(uriRetriever.fetchResponse(eq(id), any())).thenReturn(Optional.of(response));
    }

    @SuppressWarnings("unchecked")
    private void mockOrganizations(Organization org) {
        when(uriRetriever.getRawContent(any(), anyString()))
            .thenReturn(Optional.of(new CristinOrganization(org.getId(), null, null, List.of(randomCristinOrg()),
                                                           null, Map.of()).toJsonString()));
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(uriRetriever.fetchResponse(any(), anyString())).thenReturn(Optional.of(response));

    }

    private void mockOrganizationCristinResponseOnly(Organization org) {
        when(uriRetriever.getRawContent(any(), anyString()))
            .thenReturn(Optional.of(new CristinOrganization(org.getId(), null, null, List.of(randomCristinOrg()),
                                                           null, Map.of()).toJsonString()));

    }

    private CristinOrganization randomCristinOrg() {
        var partOf = List.of(new CristinOrganization(randomUri(), null, null, List.of(), null, Map.of()));
        return new CristinOrganization(randomUri(), randomUri(), randomString(), partOf, randomString(), Map.of());
    }

    public ImportCandidate randomImportCandidate(PublicationContext publicationContext) {
        return new ImportCandidate.Builder().withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription(publicationContext))
                   .withLink(randomUri())
                   .withDoi(randomDoi())
                   .withIndexedDate(Instant.now())
                   .withPublishedDate(Instant.now())
                   .withHandle(randomUri())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withSubjects(List.of(randomUri()))
                   .withIdentifier(SortableIdentifier.next())
                   .withRightsHolder(randomString())
                   .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))
                   .withFundings(List.of(new FundingBuilder().withId(randomUri()).build()))
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    @ParameterizedTest(name = "Expanded resource should inherit type from publication for instance type {0}")
    @MethodSource("publicationInstanceProvider")
    void expandedResourceShouldHaveTypePublicationInheritingTheTypeFromThePublicationWhenItIsSerialized(
        Class<?> instanceType) throws JsonProcessingException {

        var publication = randomPublication(instanceType);
        var expandedResource = fromPublication(uriRetriever, publication);
        var json = objectMapper.readTree(expandedResource.toJsonString());
        assertThat(json.get(TYPE).textValue(), is(equalTo(EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY)));
    }

    @ParameterizedTest(name = "Expanded DOI request should have type DoiRequest for instance type {0}")
    @MethodSource("publicationInstanceProvider")
    void expandedDoiRequestShouldHaveTypeDoiRequest(Class<?> instanceType) throws ApiGatewayException {
        var publication = createPublishedPublicationWithoutDoi(instanceType);
        var doiRequest = createDoiRequest(publication);
        var expandedResource = ExpandedDoiRequest.createEntry(doiRequest, resourceExpansionService, resourceService,
                                                              ticketService);
        var json = objectMapper.convertValue(expandedResource, ObjectNode.class);
        assertThat(json.get(TYPE).textValue(), is(equalTo(ExpandedDoiRequest.TYPE)));
    }

    @ParameterizedTest(name = "should return identifier using a non serializable method:{0}")
    @MethodSource("entryTypes")
    void shouldReturnIdentifierUsingNonSerializableMethod(Class<?> type)
        throws ApiGatewayException, JsonProcessingException {
        var expandedDataEntry = ExpandedDataEntryWithAssociatedPublication.create(type, resourceExpansionService,
                                                                                  resourceService, messageService,
                                                                                  ticketService, uriRetriever);
        SortableIdentifier identifier = expandedDataEntry.getExpandedDataEntry().identifyExpandedEntry();
        SortableIdentifier expectedIdentifier = extractExpectedIdentifier(expandedDataEntry);
        assertThat(identifier, is(equalTo(expectedIdentifier)));
    }

    private static Reference createReference(PublicationContext publicationContext) {
        return new Reference.Builder().withDoi(randomDoi()).withPublishingContext(publicationContext).build();
    }

    private static ExpandedDoiRequest randomDoiRequest(Publication publication,
                                                       ResourceExpansionService resourceExpansionService,
                                                       ResourceService resourceService, MessageService messageService,
                                                       TicketService ticketService) throws ApiGatewayException {
        var userInstance = UserInstance.fromPublication(publication);
        var doiRequest = (DoiRequest) TicketEntry.requestNewTicket(publication, DoiRequest.class)
                                          .persistNewTicket(ticketService);
        messageService.createMessage(doiRequest, userInstance, randomString());
        return attempt(() -> ExpandedDoiRequest.createEntry(doiRequest, resourceExpansionService, resourceService,
                                                            ticketService)).orElseThrow();
    }

    private static Publication randomPublicationWithoutDoi() {
        return randomPublication().copy().withDoi(null).build();
    }

    private static Publication randomPublicationWithoutDoi(Class<?> instanceType) {
        return randomPublication(instanceType).copy().withDoi(null).build();
    }

    private EntityDescription randomEntityDescription(PublicationContext publicationContext) {
        return new EntityDescription.Builder().withPublicationDate(
                new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .withReference(createReference(publicationContext))
                   .build();
    }

    private Contributor randomContributor() {
        Identity identity = new Identity.Builder().withId(randomUri()).withName(randomString()).build();
        return new Contributor.Builder().withIdentity(identity)
                   .withRole(new RoleType(Role.ACTOR))
                   .withAffiliations(List.of(new Organization.Builder().withId(randomUri()).build()))
                   .build();
    }

    private DoiRequest createDoiRequest(Publication publication) throws ApiGatewayException {
        return (DoiRequest) TicketEntry.requestNewTicket(publication, DoiRequest.class).persistNewTicket(ticketService);
    }

    private Publication createPublishedPublicationWithoutDoi(Class<?> instanceType) throws ApiGatewayException {
        var publication = randomPublicationWithoutDoi(instanceType);
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));
        resourceService.publishPublication(UserInstance.fromPublication(persistedPublication),
                                           persistedPublication.getIdentifier());
        return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
    }

    private SortableIdentifier extractExpectedIdentifier(ExpandedDataEntryWithAssociatedPublication generatedData) {

        ExpandedDataEntry expandedDataEntry = generatedData.getExpandedDataEntry();
        String identifier = extractIdFromSerializedObject(expandedDataEntry);
        return new SortableIdentifier(identifier);
    }

    private String extractIdFromSerializedObject(ExpandedDataEntry entry) {
        return Try.of(entry)
                   .map(ExpandedDataEntry::toJsonString)
                   .map(objectMapper::readTree)
                   .map(json -> (ObjectNode) json)
                   .map(json -> json.at(ID_JSON_PTR))
                   .map(JsonNode::textValue)
                   .map(UriWrapper::fromUri)
                   .map(UriWrapper::getLastPathElement)
                   .orElseThrow();
    }

    @Getter
    private static class ExpandedDataEntryWithAssociatedPublication {

        private final ExpandedDataEntry expandedDataEntry;

        public ExpandedDataEntryWithAssociatedPublication(ExpandedDataEntry data) {
            this.expandedDataEntry = data;
        }

        public static ExpandedDataEntryWithAssociatedPublication create(Class<?> expandedDataEntryClass,
                                                                        ResourceExpansionService expansionService,
                                                                        ResourceService resourceService,
                                                                        MessageService messageService,
                                                                        TicketService ticketService,
                                                                        UriRetriever uriRetriever)
            throws ApiGatewayException, JsonProcessingException {
            var publication = createPublication(resourceService);
            if (expandedDataEntryClass.equals(ExpandedResource.class)) {
                return createExpandedResource(publication, uriRetriever);
            } else if (expandedDataEntryClass.equals(ExpandedImportCandidate.class)) {
                return createExpandedImportCandidate(publication, uriRetriever);
            } else if (expandedDataEntryClass.equals(ExpandedDoiRequest.class)) {
                resourceService.publishPublication(UserInstance.fromPublication(publication),
                                                   publication.getIdentifier());
                var publishedPublication = resourceService.getPublication(publication);
                return new ExpandedDataEntryWithAssociatedPublication(
                    randomDoiRequest(publishedPublication, expansionService, resourceService, messageService,
                                     ticketService));
            } else if (expandedDataEntryClass.equals(ExpandedPublishingRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(
                    createExpandedPublishingRequest(publication, resourceService, expansionService,
                                                    ticketService));
            } else if (expandedDataEntryClass.equals(ExpandedGeneralSupportRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(
                    createExpandedGeneralSupportRequest(publication, resourceService, expansionService,
                                                        ticketService));
            } else if (expandedDataEntryClass.equals(ExpandedUnpublishRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(
                    createExpandedUnpublishRequest(publication, resourceService, expansionService,
                                                        ticketService));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        private static ExpandedDataEntry createExpandedGeneralSupportRequest(Publication publication,
                                                                             ResourceService resourceService,
                                                                             ResourceExpansionService expansionService,
                                                                             TicketService ticketService)
            throws NotFoundException, JsonProcessingException {
            var request = (GeneralSupportRequest) GeneralSupportRequest.fromPublication(publication);
            return ExpandedGeneralSupportRequest.create(request, resourceService, expansionService,
                                                        ticketService);
        }

        private static ExpandedDataEntry createExpandedUnpublishRequest(Publication publication,
                                                                             ResourceService resourceService,
                                                                             ResourceExpansionService expansionService,
                                                                             TicketService ticketService)
            throws NotFoundException, JsonProcessingException {
            var request = (UnpublishRequest) UnpublishRequest.fromPublication(publication);
            return ExpandedUnpublishRequest.create(request, resourceService, expansionService,
                                                        ticketService);
        }

        private static Publication createPublication(ResourceService resourceService) throws BadRequestException {
            var publication = randomPublicationWithoutDoi();
            publication = Resource.fromPublication(publication)
                              .persistNew(resourceService, UserInstance.fromPublication(publication));
            return publication;
        }

        private static ExpandedDataEntryWithAssociatedPublication createExpandedImportCandidate(
            Publication publication, UriRetriever uriRetriever) {
            var importCandidate = new Builder().withPublication(publication).build();
            var authorizedBackendClient = mock(AuthorizedBackendUriRetriever.class);
            when(authorizedBackendClient.getRawContent(any(), any())).thenReturn(Optional.of(
                new CristinOrganization(randomUri(), randomUri(), randomString(),
                                        List.of(new CristinOrganization(randomUri(),
                                                                        randomUri(),
                                                                        randomString(),
                                                                        List.of(),
                                                                        randomString(),
                                                                        Map.of())),
                                        randomString(), Map.of()).toJsonString()));
            var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
            return new ExpandedDataEntryWithAssociatedPublication(expandedImportCandidate);
        }

        private static ExpandedDataEntryWithAssociatedPublication createExpandedResource(Publication publication,
                                                                                         UriRetriever uriRetriever) {
            ExpandedResource expandedResource = attempt(() -> fromPublication(uriRetriever, publication)).orElseThrow();
            return new ExpandedDataEntryWithAssociatedPublication(expandedResource);
        }

        private static ExpandedDataEntry createExpandedPublishingRequest(Publication publication,
                                                                         ResourceService resourceService,
                                                                         ResourceExpansionService expansionService,
                                                                         TicketService ticketService)
            throws NotFoundException, JsonProcessingException {
            PublishingRequestCase requestCase = createPublishingRequestCase(publication);
            return ExpandedPublishingRequest.create(requestCase, resourceService, expansionService,
                                                    ticketService);
        }

        private static PublishingRequestCase createPublishingRequestCase(Publication publication) {
            var requestCase = new PublishingRequestCase();
            requestCase.setIdentifier(SortableIdentifier.next());
            requestCase.setStatus(TicketStatus.PENDING);
            requestCase.setModifiedDate(Instant.now());
            requestCase.setCreatedDate(Instant.now());
            requestCase.setCustomerId(publication.getPublisher().getId());
            requestCase.setResourceIdentifier(publication.getIdentifier());
            requestCase.setOwner(new User(publication.getResourceOwner().getOwner().getValue()));
            return requestCase;
        }
    }

    private record ChannelRegistryResponse(String name){}
}