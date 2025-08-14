package no.unit.nva.publication.events.handlers.persistence;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.testutils.EventBridgeEventBuilder.sampleLambdaDestinationsEvent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.FakeSqsClient;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.uriretriever.FakeUriResponse;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
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
import software.amazon.awssdk.services.s3.S3Client;

class AnalyticsIntegrationHandlerTest extends ResourcesLocalTest {

    private static final String EXPANDED_ENTRY_UPDATED_EVENT_TOPIC = "PublicationService.ExpandedDataEntry.Update";
    public static final int FILENAME_WITHOUT_FILE_ENDING = 0;
    public static final String FILENAME_AND_FILE_ENDING_SEPARATOR = "\\.";
    public static final String JSONLD_CONTEXT = "@context";
    public static final ObjectMapper DTO_OBJECT_MAPPER = JsonUtils.dtoObjectMapper;

    private AnalyticsIntegrationHandler analyticsIntegration;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;
    private ResourceService resourceService;

    private final Context context = new FakeContext();

    private TicketService ticketService;

    @BeforeEach()
    public void init() {
        super.init();
        this.outputStream = new ByteArrayOutputStream();
        S3Client s3Client = new FakeS3Client();
        this.analyticsIntegration = new AnalyticsIntegrationHandler(s3Client);
        this.s3Driver = new S3Driver(s3Client, "notImportant");

        resourceService = getResourceService(client);
        ticketService = getTicketService();
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
        var inputEvent = generateEventForExpandedPublication(AcademicArticle.class);
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

    private void assertThatAnalyticsFileHasAsFilenameThePublicationIdentifier(EventReference inputEvent,
                                                                              ExpandedResource storedPublication) {
        var expectedPublicationIdentifier = extractPublicationIdentifier(inputEvent);
        assertThat(storedPublication.identifyExpandedEntry().toString(), is(equalTo(expectedPublicationIdentifier)));
    }

    private String extractPublicationIdentifier(EventReference inputEvent) {
        return splitFilenameFromFileEnding(inputEvent)[FILENAME_WITHOUT_FILE_ENDING];
    }

    private String[] splitFilenameFromFileEnding(EventReference inputEvent) {
        return UriWrapper.fromUri(inputEvent.getUri())
                   .toS3bucketPath()
                   .getLastPathElement()
                   .split(FILENAME_AND_FILE_ENDING_SEPARATOR);
    }

    private EventReference generateEventForExpandedPublication(Class<?> type) throws IOException {
        var publication = randomPublication(type);
        var resource = super.persistResource(Resource.fromPublication(publication)).toPublication();
        var inputFileUri = expandPublicationAndSaveToS3(resource);
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

    private ExpandedDoiRequest createSampleExpandedDoiRequest() throws ApiGatewayException {
        Publication samplePublication = insertSamplePublication();
        var fakeUriRetriever = FakeUriRetriever.newInstance();
        FakeUriResponse.setupFakeForType(samplePublication, fakeUriRetriever, resourceService, false);
        var userInstance = UserInstance.fromPublication(samplePublication);
        var doiRequest = DoiRequest.create(Resource.fromPublication(samplePublication), userInstance);
        doiRequest.fetchMessages(ticketService);
        ResourceExpansionService resourceExpansionService = new ResourceExpansionServiceImpl(resourceService,
                                                                                             ticketService,
                                                                                             fakeUriRetriever,
                                                                                             fakeUriRetriever, new FakeSqsClient());
        return ExpandedDoiRequest.createEntry(doiRequest, resourceExpansionService, resourceService, ticketService);
    }

    private Publication insertSamplePublication() throws BadRequestException {
        var samplePublication = randomPublication();
        UserInstance userInstance = UserInstance.fromPublication(samplePublication);
        samplePublication = Resource.fromPublication(samplePublication).persistNew(resourceService, userInstance);
        return samplePublication;
    }

    private URI expandPublicationAndSaveToS3(Publication publication) throws IOException {
        var fakeUrlRetriever = FakeUriRetriever.newInstance();
        FakeUriResponse.setupFakeForType(publication, fakeUrlRetriever, resourceService, false);
        var expandedPublication = ExpandedResource.fromPublication(fakeUrlRetriever, resourceService, new FakeSqsClient(),
                                                                   publication);
        var resourceJson = DTO_OBJECT_MAPPER.writeValueAsString(expandedPublication);
        var randomPath = formatPublicationFilename(expandedPublication);
        return s3Driver.insertFile(randomPath, resourceJson);
    }

    private UnixPath formatPublicationFilename(ExpandedResource expandedPublication) {
        return UnixPath.of(randomString(), expandedPublication.identifyExpandedEntry() + ".gz");
    }
}