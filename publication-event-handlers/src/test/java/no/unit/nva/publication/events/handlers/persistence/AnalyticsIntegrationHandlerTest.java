package no.unit.nva.publication.events.handlers.persistence;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler.EXPANDED_ENTRY_UPDATED_EVENT_TOPIC;
import static no.unit.nva.testutils.EventBridgeEventBuilder.sampleLambdaDestinationsEvent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsIntegrationHandlerTest extends ResourcesLocalTest {

    public static final int FILENAME_WIHOUT_FILE_ENDING = 0;
    public static final String FILENAME_AND_FILE_ENDING_SEPRATOR = "\\.";
    public static final String JSONLD_CONTEXT = "@context";
    public static final Clock CLOCK = Clock.systemDefaultZone();
    public static final ObjectMapper DTO_OBJECT_MAPPER = JsonUtils.dtoObjectMapper;
    private AnalyticsIntegrationHandler analyticsIntegration;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;
    private ResourceExpansionService resourceExpansionService;
    private ResourceService resourceService;
    private AmazonDynamoDB dynamoClient;

    private final Context context = new FakeContext();

    private TicketService ticketService;

    @BeforeEach()
    public void init() {
        super.init();
        this.dynamoClient = super.client;
        this.outputStream = new ByteArrayOutputStream();
        FakeS3Client s3Client = new FakeS3Client();
        this.analyticsIntegration = new AnalyticsIntegrationHandler(s3Client);
        this.s3Driver = new S3Driver(s3Client, "notImportant");

        resourceService = new ResourceService(dynamoClient, CLOCK);
        ticketService = new TicketService(dynamoClient);

        this.resourceExpansionService = setupResourceExpansionService();
    }

    @Test
    void shouldThrowExceptionWhenEventIsOfWrongType() {
        var eventReference = new EventReference(randomString(), randomUri());
        InputStream event = sampleLambdaDestinationsEvent(eventReference);
        Executable action = () -> analyticsIntegration.handleRequest(event, outputStream, context);
        var expectedException = assertThrows(Exception.class, action);
        assertThat(expectedException.getMessage(), containsString(EXPANDED_ENTRY_UPDATED_EVENT_TOPIC));
    }

    @Test
    void shouldStoreTheExpandedPublicationReferredInTheS3UriInTheAnalyticsFolder() throws IOException {
        var inputEvent = generateEventForExpandedPublication();
        InputStream event = sampleLambdaDestinationsEvent(inputEvent);
        analyticsIntegration.handleRequest(event, outputStream, context);
        var analyticsObjectEvent = objectMapper.readValue(outputStream.toString(), EventReference.class);
        var analyticsFilePath = UriWrapper.fromUri(analyticsObjectEvent.getUri()).toS3bucketPath();
        var publicationString = s3Driver.getFile(analyticsFilePath);
        var storedPublication = DTO_OBJECT_MAPPER.readValue(publicationString, ExpandedResource.class);
        assertThat(publicationString, not(containsString(JSONLD_CONTEXT)));
        assertThatAnalyticsFileHasAsFilenameThePublicationIdentifier(inputEvent, storedPublication);
    }

    @Test
    void shouldNotStoreTheExpandedDataEntriesThatAreNotPublications() throws IOException, ApiGatewayException {
        EventReference inputEvent = generateEventForExpandedDoiRequest();
        InputStream event = sampleLambdaDestinationsEvent(inputEvent);
        analyticsIntegration.handleRequest(event, outputStream, context);
        var analyticsObjectEvent = objectMapper.readValue(outputStream.toString(), EventReference.class);
        assertThat(analyticsObjectEvent, is(nullValue()));
    }

    private ResourceExpansionServiceImpl setupResourceExpansionService() {
        return new ResourceExpansionServiceImpl(resourceService, ticketService);
    }

    private void assertThatAnalyticsFileHasAsFilenameThePublicationIdentifier(EventReference inputEvent,
                                                                              ExpandedResource storedPublication) {
        var expectedPublicationIdentifier = extractPublicationIdentifier(inputEvent);
        assertThat(storedPublication.identifyExpandedEntry().toString(), is(equalTo(expectedPublicationIdentifier)));
    }

    private String extractPublicationIdentifier(EventReference inputEvent) {
        return splitFilenameFromFileEnding(inputEvent)[FILENAME_WIHOUT_FILE_ENDING];
    }

    private String[] splitFilenameFromFileEnding(EventReference inputEvent) {
        return UriWrapper.fromUri(inputEvent.getUri())
                   .toS3bucketPath()
                   .getLastPathElement()
                   .split(FILENAME_AND_FILE_ENDING_SEPRATOR);
    }

    private EventReference generateEventForExpandedPublication() throws IOException {
        var samplePublication = PublicationGenerator.randomPublication();
        var inputFileUri = expandPublicationAndSaveToS3(samplePublication);
        return new EventReference(EXPANDED_ENTRY_UPDATED_EVENT_TOPIC, inputFileUri);
    }

    private EventReference generateEventForExpandedDoiRequest() throws IOException, ApiGatewayException {
        var expandedDoiRequest = createSampleExpandedDoiRequest();
        var expandedDoiRequestJson = DTO_OBJECT_MAPPER.writeValueAsString(expandedDoiRequest);
        var inputEventFilePath =
            UnixPath.of(randomString(), expandedDoiRequest.identifyExpandedEntry().toString());
        var fileUri = s3Driver.insertFile(inputEventFilePath, expandedDoiRequestJson);
        return new EventReference(EXPANDED_ENTRY_UPDATED_EVENT_TOPIC, fileUri);
    }

    private ExpandedDoiRequest createSampleExpandedDoiRequest() throws ApiGatewayException, JsonProcessingException {
        Publication samplePublication = insertSamplePublication();
        var doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(samplePublication));
        var messages = doiRequest.fetchMessages(ticketService);
        return ExpandedDoiRequest.createEntry(doiRequest, resourceExpansionService, resourceService, ticketService);
    }

    private Publication insertSamplePublication() throws BadRequestException {
        var samplePublication = randomPublication();
        UserInstance userInstance = UserInstance.fromPublication(samplePublication);
        samplePublication = Resource.fromPublication(samplePublication).persistNew(resourceService, userInstance);
        return samplePublication;
    }

    private URI expandPublicationAndSaveToS3(Publication publication) throws IOException {
        UriRetriever fakeUrlRetriever = mock(UriRetriever.class);
        when(fakeUrlRetriever.getRawContent(any(URI.class), anyString())).thenReturn(Optional.of(getAnthology()));
        ExpandedResource expandedPublication = ExpandedResource.fromPublication(fakeUrlRetriever, publication);
        String resourceJson = DTO_OBJECT_MAPPER.writeValueAsString(expandedPublication);
        UnixPath randomPath = formatPublicationFilename(expandedPublication);
        return s3Driver.insertFile(randomPath, resourceJson);
    }

    private UnixPath formatPublicationFilename(ExpandedResource expandedPublication) {
        return UnixPath.of(randomString(), expandedPublication.identifyExpandedEntry() + ".gz");
    }

    private String getAnthology() {
        var publication = attempt(
            () -> DTO_OBJECT_MAPPER.writeValueAsString(randomPublication(AcademicMonograph.class))).orElseThrow();
        var publicationNode = (ObjectNode) attempt(() -> DTO_OBJECT_MAPPER.readTree(publication)).orElseThrow();

        var context = Publication.getJsonLdContext(URI.create(System.getenv("ID_NAMESPACE")));
        var contextNode = attempt(() -> DTO_OBJECT_MAPPER.readTree(context)).orElseThrow();

        publicationNode.set(JSONLD_CONTEXT, contextNode);

        return attempt(() -> DTO_OBJECT_MAPPER.writeValueAsString(publicationNode)).orElseThrow();
    }
}