package no.unit.nva.expansion;

import static no.unit.nva.expansion.ResourceExpansionServiceImpl.UNSUPPORTED_TYPE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceExpansionServiceTest extends ResourcesLocalTest {

    public static final String SOME_SENDER = "some@sender";
    public static final URI SOME_ORG = URI.create("https://example.org/123");
    public static final String SOME_MESSAGE = "someMessage";
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Clock CLOCK = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());

    public static final String NOT_FOUND = "Not found";
    public static final URI RESOURCE_OWNER_UNIT_AFFILIATION = URI.create("https://api.cristin.no/v2/units/194.63.10.0");
    public static final URI RESOURCE_OWNER_INSTITUTION_AFFILIATION =
        URI.create("https://api.cristin.no/v2/institutions/194");
    private static final UserInstance SAMPLE_SENDER = sampleSender();
    private static final String PERSON_API_RESPONSE_WITH_UNIT =
        stringFromResources(Path.of("fake_person_api_response_with_unit.json"));
    private static final String PERSON_API_RESPONSE_WITH_INSTITUTION =
        stringFromResources(Path.of("fake_person_api_response_with_institution.json"));
    private static final String INSTITUTION_PROXY_ORG_RESPONSE =
        stringFromResources(Path.of("cristin_org.json"));
    private static final String INSTITUTION_PROXY_PARENT_ORG_RESPONSE =
        stringFromResources(Path.of("cristin_parent_org.json"));
    private static final String INSTITUTION_PROXY_GRAND_PARENT_ORG_RESPONSE =
        stringFromResources(Path.of("cristin_grand_parent_org.json"));
    private static final URI CRISTIN_ORG_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.63.10.0");
    private static final URI CRISTIN_ORG_PARENT_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.63.0.0");
    private static final URI CRISTIN_ORG_GRAND_PARENT_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/194.0.0.0");
    private ResourceExpansionService service;
    private ResourceService resourceService;
    private FakeHttpClient<String> externalServicesHttpClient;

    @BeforeEach
    void setUp() throws Exception {
        super.init();
        externalServicesHttpClient = new FakeHttpClient<>(PERSON_API_RESPONSE_WITH_UNIT,
                                                          INSTITUTION_PROXY_ORG_RESPONSE,
                                                          INSTITUTION_PROXY_PARENT_ORG_RESPONSE,
                                                          INSTITUTION_PROXY_GRAND_PARENT_ORG_RESPONSE
        );
        initializeServices();
    }

    @Test
    void shouldReturnExpandedMessageWithInstitutionsUriWhenPersonIsAffiliatedOnlyToInstitution() throws Exception {
        externalServicesHttpClient = new FakeHttpClient<>(PERSON_API_RESPONSE_WITH_INSTITUTION,
                                                          INSTITUTION_PROXY_GRAND_PARENT_ORG_RESPONSE
        );
        initializeServices();
        Publication createdPublication = createPublication(RESOURCE_OWNER_INSTITUTION_AFFILIATION);

        Message message = Message.supportMessage(extractUserInstance(createdPublication),
                                                 createdPublication,
                                                 randomString(),
                                                 SortableIdentifier.next(),
                                                 CLOCK);
        ExpandedMessage expandedMessage = (ExpandedMessage) service.expandEntry(message);
        assertThat(expandedMessage.getOrganizationIds(), containsInAnyOrder(CRISTIN_ORG_GRAND_PARENT_ID));
        assertThatHttpClientWasCalledOnceByAffiliationServiceAndOnceByExpansionService();
    }

    @Test
    void shouldReturnExpandedDoiRequestWithAllRelatedAffiliationsWhenResourceOwnerIsAffiliatedWithAUnit()
        throws Exception {
        Publication createdPublication = createPublication(RESOURCE_OWNER_UNIT_AFFILIATION);

        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(createdPublication));
        ExpandedDoiRequest expandedDoiRequest = (ExpandedDoiRequest) service.expandEntry(doiRequest);
        assertThat(expandedDoiRequest.getOrganizationIds(), containsInAnyOrder(
            CRISTIN_ORG_ID,
            CRISTIN_ORG_PARENT_ID,
            CRISTIN_ORG_GRAND_PARENT_ID
        ));
    }

    @Test
    void shouldReturnExpandedMessageWithAllRelatedAffiliationWhenOwnersAffiliationIsUnit() throws Exception {
        Publication createdPublication = createPublication(RESOURCE_OWNER_UNIT_AFFILIATION);

        Message message = Message.supportMessage(extractUserInstance(createdPublication),
                                                 createdPublication,
                                                 randomString(),
                                                 SortableIdentifier.next(),
                                                 CLOCK);
        ExpandedMessage expandedMessage = (ExpandedMessage) service.expandEntry(message);
        assertThat(expandedMessage.getOrganizationIds(), containsInAnyOrder(
            CRISTIN_ORG_ID,
            CRISTIN_ORG_PARENT_ID,
            CRISTIN_ORG_GRAND_PARENT_ID
        ));
    }

    @ParameterizedTest(name = "should return framed index document for resources. Instance type:{0}")
    @MethodSource("listPublicationInstanceTypes")
    void shouldReturnFramedIndexDocumentFromResource(Class<?> instanceType)
        throws JsonProcessingException, NotFoundException {
        Publication publication = PublicationGenerator.randomPublication(instanceType);
        Resource resourceUpdate = Resource.fromPublication(publication);
        ExpandedResource indexDoc = (ExpandedResource) service.expandEntry(resourceUpdate);
        assertThat(indexDoc.fetchId(), is(not(nullValue())));
    }

    @ParameterizedTest(name = "should process all ResourceUpdate types:{0}")
    @MethodSource("listResourceUpdateTypes")
    void shouldProcessAllResourceUpdateTypes(Class<?> resourceUpdateType)
        throws IOException, ApiGatewayException {
        DataEntry resource = generateResourceUpdate(resourceUpdateType);
        service.expandEntry(resource);
        assertDoesNotThrow(() -> service.expandEntry(resource));
    }

    private static List<Class<?>> listResourceUpdateTypes() {
        JsonSubTypes[] annotations = DataEntry.class.getAnnotationsByType(JsonSubTypes.class);
        Type[] types = annotations[0].value();
        return Arrays.stream(types).map(Type::value).collect(Collectors.toList());
    }

    private static List<Class<?>> listPublicationInstanceTypes() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes();
    }

    private static UserInstance sampleSender() {
        return new UserInstance(SOME_SENDER, SOME_ORG);
    }

    private void initializeServices() {
        resourceService = new ResourceService(client, externalServicesHttpClient, Clock.systemDefaultZone());
        service = new ResourceExpansionServiceImpl(externalServicesHttpClient, resourceService);
    }

    private void assertThatHttpClientWasCalledOnceByAffiliationServiceAndOnceByExpansionService() {
        assertThat(externalServicesHttpClient.getCallCounter().get(), is(equalTo(2)));
    }

    private Publication createPublication(URI resourceOwnerAffiliation) throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        UserInstance userInstance = extractUserInstance(publication);
        var createdPublication = resourceService.createPublication(userInstance, publication);
        assertThat(createdPublication.getResourceOwner().getOwnerAffiliation(), is(equalTo(
            resourceOwnerAffiliation)));
        return createdPublication;
    }

    private DataEntry generateResourceUpdate(Class<?> resourceUpdateType) throws ApiGatewayException {
        Publication createdPublication = createPublication(RESOURCE_OWNER_UNIT_AFFILIATION);
        if (Resource.class.equals(resourceUpdateType)) {
            return Resource.fromPublication(createdPublication);
        }
        if (DoiRequest.class.equals(resourceUpdateType)) {
            return createDoiRequest(createdPublication);
        }
        if (Message.class.equals(resourceUpdateType)) {
            return createMessage(createdPublication);
        }
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + resourceUpdateType.getSimpleName());
    }

    private DoiRequest createDoiRequest(Publication createdPublication) {
        Resource resource = Resource.fromPublication(createdPublication);

        return DoiRequest.newDoiRequestForResource(resource);
    }

    private Message createMessage(Publication createdPublication) {
        SortableIdentifier messageIdentifier = SortableIdentifier.next();
        return Message.supportMessage(SAMPLE_SENDER, createdPublication, SOME_MESSAGE, messageIdentifier, CLOCK);
    }
}
