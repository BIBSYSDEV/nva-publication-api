package no.unit.nva.publication.events.handlers.tickets;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;
import no.unit.nva.publication.exception.InvalidInputException;
import no.unit.nva.publication.model.business.*;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent.*;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.tickets.DoiRequestEventProducer.*;
import static no.unit.nva.testutils.RandomDataGenerator.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class DoiRequestEventProducerTest extends ResourcesLocalTest {

    public static final Username USERNAME = new Username(randomString());
    private DoiRequestEventProducer handler;
    private Context context;
    private ByteArrayOutputStream outputStream;
    private ResourceService resourceService;
    private FakeHttpClient<String> httpClient;
    private FakeS3Client s3Client;

    public static Stream<Function<Publication, Entity>> entityProvider() {
        return Stream.of(Resource::fromPublication, DoiRequest::fromPublication);
    }

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        var response = FakeHttpResponse.create(randomString(), HttpURLConnection.HTTP_OK);
        this.httpClient = new FakeHttpClient<>(response);
        s3Client = new FakeS3Client();
        handler = new DoiRequestEventProducer(resourceService, httpClient, s3Client);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void handleRequestThrowsExceptionWhenEventContainsResourceUpdateThatCannotBeReferenced()
            throws ApiGatewayException, IOException {

        var doiRequestWithoutIdentifier = sampleDoiRequestForExistingPublication();
        doiRequestWithoutIdentifier.setIdentifier(null);
        try (var event = createEvent(null, doiRequestWithoutIdentifier)) {
            Executable action = () -> handler.handleRequest(event, outputStream, context);
            IllegalStateException exception = assertThrows(IllegalStateException.class, action);
            assertThat(exception.getMessage(), is(equalTo(DOI_REQUEST_HAS_NO_IDENTIFIER)));
        }
    }

    @Test
    void handleRequestThrowsExceptionWhenEventIsADeletion() throws IOException {
        var publication = randomPublishedPublication();
        try (var event = createEvent(Resource.fromPublication(publication), Resource.fromPublication(null))) {
            Executable action = () -> handler.handleRequest(event, outputStream, context);
            InvalidInputException exception = assertThrows(InvalidInputException.class, action);
            assertThat(exception.getMessage(), is(equalTo(HANDLER_DOES_NOT_DEAL_WITH_DELETIONS)));
        }
    }

    @Test
    void handleRequestThrowsExceptionWhenEventContainsResourceUpdateWithoutReferenceToResource()
            throws ApiGatewayException, IOException {

        var doiRequestWithoutResourceIdentifier = sampleDoiRequestForExistingPublication();
        doiRequestWithoutResourceIdentifier.setPublicationDetails(null);
        try (var event = createEvent(null, doiRequestWithoutResourceIdentifier)) {
            Executable action = () -> handler.handleRequest(event, outputStream, context);
            IllegalStateException exception = assertThrows(IllegalStateException.class, action);
            assertThat(exception.getMessage(), is(equalTo(TicketEntry.TICKET_WITHOUT_REFERENCE_TO_PUBLICATION_ERROR)));
        }
    }

    @Test
    void shouldNotPropagateEventWhenThereIsNoDoiRequestButThePublicationHasBeenAssignedANonFindableDoiByNvaPredecessor()
            throws IOException, BadRequestException {
        //Given a publication has a public Doi
        var publication = persistPublicationWithDoi();
        var publicationUpdate = updateTitle(publication);
        assertThat(publication.getDoi(), is(not(nullValue())));
        assertThat(publicationUpdate.getDoi(), is(equalTo(publication.getDoi())));

        var event = createEvent(Resource.fromPublication(publication), Resource.fromPublication(publicationUpdate));
        this.httpClient = new FakeHttpClient<>(FakeHttpResponse.create(null, HTTP_NOT_FOUND));
        this.handler = new DoiRequestEventProducer(resourceService, httpClient, s3Client);
        handler.handleRequest(event, outputStream, context);
        var actual = outputToPublicationHolder(outputStream);

        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    @Test
    void handlerCreatesUpdateDoiEventWhenPublicationHasAFindableDoi()
            throws IOException {
        var publication = randomPublishedPublication();
        var updatedPublication = updateTitle(publication);
        var event = createEvent(Resource.fromPublication(publication),
                Resource.fromPublication(updatedPublication));
        this.httpClient = new FakeHttpClient<>(findableDoiResponse());
        this.handler = new DoiRequestEventProducer(resourceService, httpClient, s3Client);

        handler.handleRequest(event, outputStream, context);
        var actual = outputToPublicationHolder(outputStream);

        assertThat(actual.getTopic(), is(equalTo(UPDATE_DOI_EVENT_TOPIC)));
        var expectedPublicationId = inferExpectedPublicationId(publication);
        var expectedCustomerId = extractExpectedCustomerId(publication);
        assertThat(actual, allOf(
                hasProperty(PUBLICATION_ID, equalTo(expectedPublicationId)),
                hasProperty(CUSTOMER_ID, equalTo(expectedCustomerId)))
        );
    }

    @Test
    void shouldCreateUpdateEventWhenPublicationHasNoDoiAndADraftDoiRequestGetsApproved()
            throws IOException,
            ApiGatewayException {
        var publication = persistPublicationWithoutDoi(PublicationStatus.PUBLISHED);
        var draftRequest = DoiRequest.fromPublication(publication);
        var approvedRequest = draftRequest.complete(publication, USERNAME);
        var event = createEvent(draftRequest, approvedRequest);

        handler.handleRequest(event, outputStream, context);
        var actual = outputToPublicationHolder(outputStream);
        assertThat(actual.getTopic(), is(equalTo(UPDATE_DOI_EVENT_TOPIC)));
        var expectedPublicationId = inferExpectedPublicationId(publication);
        var expectedCustomerId = extractExpectedCustomerId(publication);
        assertThat(actual, allOf(
                hasProperty(PUBLICATION_ID, equalTo(expectedPublicationId)),
                hasProperty(CUSTOMER_ID, equalTo(expectedCustomerId)))
        );
    }

    @Test
    void shouldNotCreateEventForPublicationsWithoutDoi() throws IOException, ApiGatewayException {
        var publication = persistPublicationWithoutDoi();
        var publicationUpdate = publication.copy().withModifiedDate(randomInstant()).build();
        assertThat(publication.getModifiedDate(), is(not(equalTo(publicationUpdate.getModifiedDate()))));

        var updateEvent = createEvent(
                Resource.fromPublication(publication),
                Resource.fromPublication(publicationUpdate));
        handler.handleRequest(updateEvent, outputStream, context);

        var emittedEvent = outputToPublicationHolder(outputStream);
        assertThat(emittedEvent, is(equalTo(EMPTY_EVENT)));
    }

    @ParameterizedTest(name = "should ignore events when old and new image are identical")
    @MethodSource("entityProvider")
    void shouldIgnoreEventsWhenNewAndOldImageAreIdentical(Function<Publication, Entity> entityProvider)
            throws IOException, BadRequestException {
        var publication = persistPublicationWithDoi();
        var entity = entityProvider.apply(publication);
        var event = createEvent(entity, entity);
        handler.handleRequest(event, outputStream, context);
        var actual = outputToPublicationHolder(outputStream);

        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    @Test
    void shouldNotSendRequestForDraftingADoiWhenThereHasBeenVeryRecentPreviousDoiRequestButNoDoiHasBeenCreated()
            throws ApiGatewayException, IOException {
        var publication = persistPublicationWithoutDoi();
        var oldDoiRequest = DoiRequest.fromPublication(publication);
        var tooShortIntervalForRerequesting = MIN_INTERVAL_FOR_REREQUESTING_A_DOI.minusSeconds(1);
        oldDoiRequest.setModifiedDate(oldDoiRequest.getModifiedDate().minus(tooShortIntervalForRerequesting));
        var newDoiRequest = DoiRequest.fromPublication(publication);
        var event = createEvent(oldDoiRequest, newDoiRequest);
        handler.handleRequest(event, outputStream, context);
        var actual = outputToPublicationHolder(outputStream);

        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    @Test
    void shouldNotEmitEventForPublicationMissingRequiredFields() throws IOException {
        var publication = createPublicationWithAllRequiredFieldsSetToFaulty();
        var updatedPublication = updateDescription(publication);
        var event = createEvent(Resource.fromPublication(publication),
                Resource.fromPublication(updatedPublication));
        this.httpClient = new FakeHttpClient<>(findableDoiResponse());
        this.handler = new DoiRequestEventProducer(resourceService, httpClient, s3Client);

        handler.handleRequest(event, outputStream, context);
        var actual = outputToPublicationHolder(outputStream);

        assertThat(actual, is(equalTo(EMPTY_EVENT)));
    }

    private Publication updateDescription(Publication publication) {
        var publicationUpdate = publication.copy().build();
        publicationUpdate.getEntityDescription().setDescription(randomString());
        return publicationUpdate;
    }

    private Publication createPublicationWithAllRequiredFieldsSetToFaulty() {
        var publication = randomPublication();
        publication.setStatus(PublicationStatus.DRAFT);
        publication.getEntityDescription().setMainTitle(StringUtils.EMPTY_STRING);
        publication.getEntityDescription().getPublicationDate().setYear(null);
        publication.getEntityDescription().getReference().setPublicationInstance(null);
        publication.setPublisher(null);
        publication.setModifiedDate(null);
        return publication;
    }

    private Publication randomPublishedPublication() {
        var publication = randomPublication();
        publication.setStatus(PublicationStatus.PUBLISHED);
        return publication;
    }

    private URI extractExpectedCustomerId(Publication publication) {
        return publication.getPublisher().getId();
    }

    private URI inferExpectedPublicationId(Publication publication) {
        return UriWrapper.fromUri(NVA_API_DOMAIN)
                .addChild("publication")
                .addChild(publication.getIdentifier().toString())
                .getUri();
    }

    private InputStream createEvent(Entity oldImage, Entity newImage) throws IOException {
        var dataEntry = createDataEntry(oldImage, newImage);
        var s3driver = new S3Driver(s3Client, "ignored");
        var content = dataEntry.toJsonString();
        var eventBlobUri = s3driver.insertEvent(UnixPath.EMPTY_PATH, content);
        var eventReference = new EventReference(randomString(), eventBlobUri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventReference);
    }

    private DataEntryUpdateEvent createDataEntry(Entity draftRequest, Entity approvedRequest) {
        return new DataEntryUpdateEvent(randomString(), draftRequest, approvedRequest);
    }

    private Publication persistPublicationWithDoi() throws BadRequestException {
        return persistPublication(randomPublication());
    }

    private Publication persistPublicationWithoutDoi() throws ApiGatewayException {
        return persistPublicationWithoutDoi(randomElement(PublicationStatus.values()));
    }

    private Publication persistPublicationWithoutDoi(PublicationStatus publicationStatus) throws ApiGatewayException {
        var publication = randomPublication();
        publication.setDoi(null);
        var persistedPublication = persistPublication(publication);

        if (PublicationStatus.PUBLISHED.equals(publicationStatus)) {
            resourceService.publishPublication(UserInstance.fromPublication(persistedPublication),
                    persistedPublication.getIdentifier());
        }
        return resourceService.getPublication(persistedPublication);
    }

    private Publication updateTitle(Publication publication) {
        var publicationUpdate = publication.copy().build();
        publicationUpdate.setEntityDescription(randomPublication().getEntityDescription());
        return publicationUpdate;
    }

    private FakeHttpResponse<String> findableDoiResponse() {
        return FakeHttpResponse.create(null, HTTP_FOUND);
    }

    private DoiRequest sampleDoiRequestForExistingPublication() throws ApiGatewayException {
        var publication = persistPublicationWithoutDoi();
        return DoiRequest.fromPublication(publication);
    }

    private Publication persistPublication(Publication publication) throws BadRequestException {
        return Resource.fromPublication(publication).persistNew(resourceService,
                UserInstance.fromPublication(publication));
    }

    private DoiMetadataUpdateEvent outputToPublicationHolder(ByteArrayOutputStream outputStream)
            throws JsonProcessingException {
        String outputString = outputStream.toString();
        return objectMapper.readValue(outputString, DoiMetadataUpdateEvent.class);
    }
}