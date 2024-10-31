package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.ID_JSON_PTR;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import no.unit.nva.expansion.JournalExpansionServiceImpl;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.identifiers.SortableIdentifier;
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
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
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
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
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
import no.unit.nva.publication.uriretriever.FakeUriResponse;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDataEntryTest extends ResourcesLocalTest {

    public static final String TYPE = "type";
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY = "Publication";
    public static final Book BOOK_SAMPLE = new Book(null, randomString(), new Publisher(randomUri()), List.of(),
                                                    Revision.UNREVISED);

    private ResourceExpansionService resourceExpansionService;
    private ResourceService resourceService;
    private TicketService ticketService;
    private MessageService messageService;
    private FakeUriRetriever uriRetriever;

    public static Stream<Named<Class<?>>> entryTypes() {
        return TypeProvider.listSubTypes(ExpandedDataEntry.class);
    }

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    public static Stream<PublicationContext> importCandidateContextTypeProvider()
        throws InvalidUnconfirmedSeriesException {
        return Stream.of(BOOK_SAMPLE,
                         new Report(null, randomString(), null, null, List.of()),
                         new MediaContributionPeriodical(randomUri()));
    }

    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = getResourceServiceBuilder().build();
        this.messageService = getMessageService();
        this.ticketService = getTicketService();
        this.uriRetriever = FakeUriRetriever.newInstance();
        this.resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, ticketService, uriRetriever
            , uriRetriever);
    }

    @ParameterizedTest()
    @MethodSource("importCandidateContextTypeProvider")
    void shouldExpandImportCandidateSuccessfully(PublicationContext publicationContext) {
        var importCandidate = randomImportCandidate(publicationContext);
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);

        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
    }


    @Test
    void shouldExpandImportCandidateCristinOrgWhenAffiliatedWithNvaCustomer() {
        final var logger = LogUtils.getTestingAppenderForRootLogger();
        var importCandidate = randomImportCandidate(BOOK_SAMPLE);
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);
        importCandidate.getEntityDescription().getContributors().stream()
            .map(Contributor::getAffiliations)
                .flatMap(i -> i.stream()
                              .filter(Organization.class::isInstance)
                              .map(Organization.class::cast)
                                  .map(Organization::getId))
                    .forEach(this::addResponsesForCristinCustomer);
        ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(logger.getMessages(), containsString("is nva customer: true"));
    }

    private void addResponsesForCristinCustomer(URI uri) {
        uriRetriever.registerResponse(toCristinOrgUri(UriWrapper.fromUri(uri).getLastPathElement()), SC_OK,
                                      MediaType.ANY_APPLICATION_TYPE,
                                      FakeUriResponse.createCristinOrganizationResponseForTopLevelOrg(uri));
        uriRetriever.registerResponse(toFetchCustomerByCristinIdUri(uri), SC_OK,
                                      MediaType.ANY_APPLICATION_TYPE, "{}");
    }

    private static URI toFetchCustomerByCristinIdUri(URI topLevelOrganization) {
        var getCustomerEndpoint = UriWrapper.fromHost(API_HOST).addChild("customer").addChild("cristinId").getUri();
        return URI.create(
            getCustomerEndpoint + "/" + URLEncoder.encode(topLevelOrganization.toString(), StandardCharsets.UTF_8));
    }

    private static URI toCristinOrgUri(String cristinId) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild("cristin")
                   .addChild("organization")
                   .addQueryParameter("depth", "top")
                   .addChild(cristinId).getUri();
    }

    @Test
    void shouldLogErrorWhenResponseFromChannelRegistryIsNotOk() {
        final var logger = LogUtils.getTestingAppenderForRootLogger();
        var importCandidate = randomImportCandidate(BOOK_SAMPLE);
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);
        var channelUri =
            ((Publisher)((Book) importCandidate.getEntityDescription().getReference().getPublicationContext())
                            .getPublisher()).getId();
        uriRetriever.registerResponse(channelUri, SC_FORBIDDEN, MediaType.ANY_APPLICATION_TYPE, randomString());
        ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(logger.getMessages(), containsString("Not Ok response from channel registry"));
    }

    @Test
    void shouldLogErrorWhenResponseFromChannelRegistryResponseIsNonsense() {
        final var logger = LogUtils.getTestingAppenderForRootLogger();
        var importCandidate = randomImportCandidate(BOOK_SAMPLE);
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);
        var channelUri =
            ((Publisher)((Book) importCandidate.getEntityDescription().getReference().getPublicationContext())
                            .getPublisher()).getId();
        uriRetriever.registerResponse(channelUri, SC_OK, MediaType.ANY_APPLICATION_TYPE, randomString());
        ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(logger.getMessages(), containsString("Failed to parse channel registry response"));
    }

    @Test
    void shouldExpandImportCandidateJournalSuccessfullyWhenBadResponseFromChannelRegistry() {
        final var logAppender = LogUtils.getTestingAppender(JournalExpansionServiceImpl.class);
        var journalId = randomUri();
        var journalContext = new Journal(journalId);
        var importCandidate = randomImportCandidate(journalContext);
        overrideStandardResponseWithNotFoundFromChannelRegistry(importCandidate, journalId);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getJournal(), is(equalTo(new ExpandedJournal(journalId, null))));
        assertThat(logAppender.getMessages(), containsString("Not Ok response from channel registry"));
    }

    private void overrideStandardResponseWithNotFoundFromChannelRegistry(ImportCandidate importCandidate,
                                                                         URI journalId) {
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);
        uriRetriever.registerResponse(journalId, SC_NOT_FOUND, MediaType.ANY_APPLICATION_TYPE, "");
    }

    @Test
    void shouldExpandJournalSuccessfullyWhenOkResponseFromChannelRegistry() {
        var journalId = randomUri();
        var journalContext = new Journal(journalId);
        var importCandidate = randomImportCandidate(journalContext);
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getJournal().name(), is(notNullValue()));
    }

    @Test
    void shouldExpandPublisherSuccessfullyWhenBadResponseFromChannelRegistry() {
        var publisherId = randomUri();
        var publisher = new Publisher(publisherId);
        var bookContext = new Book(null, null, publisher, null, null);
        var importCandidate = randomImportCandidate(bookContext);
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getPublisher().name(), is(notNullValue()));
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
    void shouldExpandPublisherSuccessfullyWhenOkResponseFromChannelRegistry() {
        var publisherId = randomUri();
        var publisher = new Publisher(publisherId);
        var bookContext = new Book(null, null, publisher, null, null);
        var importCandidate = randomImportCandidate(bookContext);
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));

        assertThat(expandedImportCandidate.getPublisher().name(), is(notNullValue()));
    }

    @Test
    void shouldLogFailureToParseChannelRegistryResponse() {
        final var logAppender = LogUtils.getTestingAppender(JournalExpansionServiceImpl.class);
        var journalId = randomUri();
        var journalContext = new Journal(journalId);
        var importCandidate = randomImportCandidate(journalContext);
        FakeUriResponse.setupFakeForType(importCandidate, uriRetriever, resourceService);
        overrideDefaultFakeResponseToReturnNonsensicalResponse(importCandidate);
        var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate, uriRetriever);
        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        assertThat(expandedImportCandidate.getJournal(), is(equalTo(new ExpandedJournal(journalId, null))));
        assertThat(logAppender.getMessages(), containsString("Failed to parse channel registry response"));
    }

    private void overrideDefaultFakeResponseToReturnNonsensicalResponse(ImportCandidate importCandidate) {
        var journalUri =
            ((Journal) importCandidate.getEntityDescription().getReference().getPublicationContext()).getId();
        uriRetriever.registerResponse(journalUri, SC_OK, MediaType.ANY_APPLICATION_TYPE, randomString());
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
        var fakeUriRetriever = FakeUriRetriever.newInstance();

        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        var expandedResource = fromPublication(fakeUriRetriever, resourceService, publication);
        var json = objectMapper.readTree(expandedResource.toJsonString());
        assertThat(json.get(TYPE).textValue(), is(equalTo(EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY)));
    }

    @ParameterizedTest(name = "Expanded DOI request should have type DoiRequest for instance type {0}")
    @MethodSource("publicationInstanceProvider")
    void expandedDoiRequestShouldHaveTypeDoiRequest(Class<?> instanceType) throws ApiGatewayException {
        var publication = createPublishedPublicationWithoutDoi(instanceType);
        FakeUriResponse.setupFakeForType(publication, uriRetriever);
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

        var expandedDataEntry = new ExpandedDataEntryWithAssociatedPublication().create(type, resourceExpansionService,
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
    private class ExpandedDataEntryWithAssociatedPublication {

        private final ExpandedDataEntry expandedDataEntry;

        public ExpandedDataEntryWithAssociatedPublication(ExpandedDataEntry data) {
            this.expandedDataEntry = data;
        }

        public ExpandedDataEntryWithAssociatedPublication() {
            this.expandedDataEntry = null;
        }

        public ExpandedDataEntryWithAssociatedPublication create(Class<?> expandedDataEntryClass,
                                                                        ResourceExpansionService expansionService,
                                                                        ResourceService resourceService,
                                                                        MessageService messageService,
                                                                        TicketService ticketService,
                                                                        RawContentRetriever uriRetriever)
            throws ApiGatewayException, JsonProcessingException {
            var publication = createPublication(resourceService);
            FakeUriResponse.setupFakeForType(publication, (FakeUriRetriever) uriRetriever, resourceService);
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
            var publication = randomPublicationWithoutDoi(AcademicArticle.class);
            publication = Resource.fromPublication(publication)
                              .persistNew(resourceService, UserInstance.fromPublication(publication));
            return publication;
        }

        private ExpandedDataEntryWithAssociatedPublication createExpandedImportCandidate(
            Publication publication, RawContentRetriever uriRetriever) {
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

        private ExpandedDataEntryWithAssociatedPublication createExpandedResource(
            Publication publication,
            RawContentRetriever uriRetriever) {
            ExpandedResource expandedResource =
                attempt(() -> fromPublication(uriRetriever, resourceService, publication)).orElseThrow();
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
}