package no.unit.nva.publication.events.handlers.create;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.create.CreatePublicationRequest;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.RandomPersonServiceResponse;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatePublishedPublicationHandlerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final String DATA_IMPORT_TOPIC = "PublicationService.DataImport.DataEntry";
    public static final String SCOPUS_IMPORT_SUBTOPIC = "PublicationService.ScopusData.DataEntry";
    private CreatePublishedPublicationHandler handler;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;
    private ResourceService publicationService;

    @BeforeEach
    public void init() {
        super.init();
        this.outputStream = new ByteArrayOutputStream();
        FakeS3Client fakeS3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(fakeS3Client, "notimportant");
        FakeHttpClient<String> httpClient = new FakeHttpClient<>(new RandomPersonServiceResponse().toString());
        this.handler = new CreatePublishedPublicationHandler(fakeS3Client, client, httpClient);
        this.publicationService = new ResourceService(super.client, httpClient, Clock.systemDefaultZone());
    }

    @Test
    void shouldReceiveAnEventReferenceAndReadFileFromS3() throws IOException {
        var samplePublication = PublicationGenerator.randomPublication();
        var s3FileUri = createPublicationRequestAndStoreInS3(samplePublication);

        var response = sendMessageToEventHandler(s3FileUri);

        var actualSampleValue = response.getEntityDescription().getMainTitle();
        var expectedSampleValue = samplePublication.getEntityDescription().getMainTitle();
        assertThat(actualSampleValue, is(equalTo(expectedSampleValue)));
    }

    @Test
    void shouldStoreTheCreatePublicationRequestReferencedByTheEventAsPublishedPublication()
        throws IOException, NotFoundException {
        Publication samplePublication = createSamplePublicationNotContainingFieldThatScopusWillNotProvide();
        var s3FileUri = createPublicationRequestAndStoreInS3(samplePublication);
        sendMessageToEventHandler(s3FileUri);
        var savedPublication = extractSavedPublicationFromDatabase();

        var expectedPublication = copyFieldsCreatedByHandler(samplePublication.copy(), savedPublication)
            .withStatus(PublicationStatus.PUBLISHED)
            .build();

        assertThat(savedPublication, is(equalTo(expectedPublication)));

    }

    private Publication extractSavedPublicationFromDatabase() throws JsonProcessingException, NotFoundException {
        var savedPublicationIdentifier = parseResponse(outputStream.toString()).getIdentifier();
        return this.publicationService.getPublicationByIdentifier(savedPublicationIdentifier);
    }

    private Publication createSamplePublicationNotContainingFieldThatScopusWillNotProvide() {
        var samplePublication = PublicationGenerator.randomPublication();
        samplePublication =
            deleteFieldsThatAreExpectedToBeNullWhenCreatingAPublishedPublicationFromScopus(samplePublication);
        return samplePublication;
    }

    private URI createPublicationRequestAndStoreInS3(Publication samplePublication) throws IOException {
        var request = CreatePublicationRequest.fromPublication(samplePublication);
        return storeRequestInS3(request);
    }

    private Publication deleteFieldsThatAreExpectedToBeNullWhenCreatingAPublishedPublicationFromScopus(
        Publication publication) {
        return publication.copy()
            .withDoi(null)
            .withDoiRequest(null)
            .withHandle(null)
            .withPublishedDate(null)
            .withLink(null)
            .withIndexedDate(null)
            .build();
    }

    private Publication.Builder copyFieldsCreatedByHandler(Publication.Builder publicationBuilder,
                                                           Publication savedPublication) {
        return publicationBuilder.withResourceOwner(savedPublication.getResourceOwner())
            .withPublisher(savedPublication.getPublisher())
            .withCreatedDate(savedPublication.getCreatedDate())
            .withModifiedDate(savedPublication.getModifiedDate())
            .withIdentifier(savedPublication.getIdentifier());
    }

    private PublicationResponse sendMessageToEventHandler(URI s3FileUri) throws JsonProcessingException {
        var eventBody = new EventReference(DATA_IMPORT_TOPIC, SCOPUS_IMPORT_SUBTOPIC, s3FileUri);
        var event = EventBridgeEventBuilder.sampleEvent(eventBody);
        handler.handleRequest(event, outputStream, CONTEXT);
        return parseResponse(outputStream.toString());
    }

    private URI storeRequestInS3(CreatePublicationRequest request) throws IOException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(request);
        return s3Driver.insertFile(UnixPath.of(randomString()), json);
    }

    private PublicationResponse parseResponse(String responseString) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(responseString, PublicationResponse.class);
    }
}
