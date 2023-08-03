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
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.MediaContributionPeriodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIsbnException;
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
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

class ExpandedDataEntryTest extends ResourcesLocalTest {

    public static final String TYPE = "type";
    public static final String EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY = "Publication";

    private ResourceExpansionService resourceExpansionService;
    private ResourceService resourceService;
    private TicketService ticketService;
    private MessageService messageService;
    private UriRetriever uriRetriever;
    private AuthorizedBackendUriRetriever authorizedBackendUriRetriever;

    public static Stream<Class<?>> entryTypes() {
        return TypeProvider.listSubTypes(ExpandedDataEntry.class);
    }

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    public static Stream<PublicationContext> importCandidateContextTypeProvider()
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return Stream.of(new Book(null, randomString(), new Publisher(randomUri()), List.of()),
                         new Report(null, randomString(), null, null, List.of()),
                         new MediaContributionPeriodical(randomUri()));
    }

    @BeforeEach
    public void setup() {
        super.init();
        var clock = Clock.systemDefaultZone();
        this.resourceService = new ResourceService(client, clock);
        this.messageService = new MessageService(client);
        this.ticketService = new TicketService(client);
        this.uriRetriever = mock(UriRetriever.class);
        this.authorizedBackendUriRetriever = mock(AuthorizedBackendUriRetriever.class);
        when(uriRetriever.getRawContent(any(), any())).thenReturn(Optional.empty());

        this.resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, ticketService);
    }

    @ParameterizedTest()
    @MethodSource("importCandidateContextTypeProvider")
    public void shouldExpandImportCandidateSuccessfully(PublicationContext publicationContext) {
        var importCandidate = randomImportCandidate(publicationContext);
        when(authorizedBackendUriRetriever.getRawContent(any(), any())).thenReturn(Optional.of(randomString()));
        var expandedImportCandidate = ExpandedImportCandidate
                                          .fromImportCandidate(importCandidate, authorizedBackendUriRetriever);

        assertThat(importCandidate.getIdentifier(), is(equalTo(expandedImportCandidate.identifyExpandedEntry())));
        this.resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, ticketService);
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

    @ParameterizedTest(name = "Expanded resource should not lose information for instance type {0}")
    @MethodSource("publicationInstanceProvider")
    void shouldReturnExpandedResourceWithoutLossOfInformation(Class<?> instanceType)
        throws JsonProcessingException, BadRequestException, JSONException {
        var publication = createPublicationWithoutDoi(instanceType);
        var expandedResource = fromPublication(uriRetriever, publication);

        var expandedResourceAsJson = expandedResource.toJsonString();

        var regeneratedPublication = objectMapper.readValue(expandedResourceAsJson, Publication.class);

        var jsonOriginal = attempt(() -> objectMapper.writeValueAsString(publication)).orElseThrow();
        var jsonActual = attempt(() -> objectMapper.writeValueAsString(regeneratedPublication)).orElseThrow();

        JSONAssert.assertEquals(jsonOriginal, jsonActual, false);
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
        return new Contributor.Builder().withIdentity(new Identity.Builder().withId(randomUri()).withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withAffiliations(List.of(new Organization.Builder().withId(randomUri()).build()))
                   .build();
    }

    private DoiRequest createDoiRequest(Publication publication) throws ApiGatewayException {
        return (DoiRequest) TicketEntry.requestNewTicket(publication, DoiRequest.class).persistNewTicket(ticketService);
    }

    private Publication createPublicationWithoutDoi(Class<?> instanceType) throws BadRequestException {
        var publication = randomPublicationWithoutDoi(instanceType);
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
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
                                                                        ResourceExpansionService resourceExpansionService,
                                                                        ResourceService resourceService,
                                                                        MessageService messageService,
                                                                        TicketService ticketService,
                                                                        UriRetriever uriRetriever)
            throws ApiGatewayException, JsonProcessingException {
            var publication = createPublication(resourceService);
            if (expandedDataEntryClass.equals(ExpandedResource.class)) {
                return createExpandedResource(publication, uriRetriever);
            } else if (expandedDataEntryClass.equals(ExpandedImportCandidate.class)) {
                return createExpandedImportCandidate(publication);
            } else if (expandedDataEntryClass.equals(ExpandedDoiRequest.class)) {
                resourceService.publishPublication(UserInstance.fromPublication(publication),
                                                   publication.getIdentifier());
                var publishedPublication = resourceService.getPublication(publication);
                return new ExpandedDataEntryWithAssociatedPublication(
                    randomDoiRequest(publishedPublication, resourceExpansionService, resourceService, messageService,
                                     ticketService));
            } else if (expandedDataEntryClass.equals(ExpandedPublishingRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(
                    createExpandedPublishingRequest(publication, resourceService, resourceExpansionService,
                                                    ticketService));
            } else if (expandedDataEntryClass.equals(ExpandedGeneralSupportRequest.class)) {
                return new ExpandedDataEntryWithAssociatedPublication(
                    createExpandedGeneralSupportRequest(publication, resourceService, resourceExpansionService,
                                                        ticketService));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        private static ExpandedDataEntry createExpandedGeneralSupportRequest(Publication publication,
                                                                             ResourceService resourceService,
                                                                             ResourceExpansionService resourceExpansionService,
                                                                             TicketService ticketService)
            throws NotFoundException, JsonProcessingException {
            var request = (GeneralSupportRequest) GeneralSupportRequest.fromPublication(publication);
            return ExpandedGeneralSupportRequest.create(request, resourceService, resourceExpansionService,
                                                        ticketService);
        }

        private static Publication createPublication(ResourceService resourceService) throws BadRequestException {
            var publication = randomPublicationWithoutDoi();
            publication = Resource.fromPublication(publication)
                              .persistNew(resourceService, UserInstance.fromPublication(publication));
            return publication;
        }

        private static ExpandedDataEntryWithAssociatedPublication createExpandedImportCandidate(
            Publication publication) {
            var importCandidate = new Builder().withPublication(publication).build();
            var authorizedBackendClient = mock(AuthorizedBackendUriRetriever.class);
            when(authorizedBackendClient.getRawContent(any(), any())).thenReturn(Optional.of(
                new CristinOrganization(List.of(new Organization.Builder().withId(randomUri()).build())).toJsonString()
                + " 200"));
            var expandedImportCandidate = ExpandedImportCandidate.fromImportCandidate(importCandidate,
                                                                                      authorizedBackendClient);
            return new ExpandedDataEntryWithAssociatedPublication(expandedImportCandidate);
        }

        private static ExpandedDataEntryWithAssociatedPublication createExpandedResource(Publication publication,
                                                                                         UriRetriever uriRetriever) {
            ExpandedResource expandedResource = attempt(() -> fromPublication(uriRetriever, publication)).orElseThrow();
            return new ExpandedDataEntryWithAssociatedPublication(expandedResource);
        }

        private static ExpandedDataEntry createExpandedPublishingRequest(Publication publication,
                                                                         ResourceService resourceService,
                                                                         ResourceExpansionService resourceExpansionService,
                                                                         TicketService ticketService)
            throws NotFoundException, JsonProcessingException {
            PublishingRequestCase requestCase = createPublishingRequestCase(publication);
            return ExpandedPublishingRequest.create(requestCase, resourceService, resourceExpansionService,
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