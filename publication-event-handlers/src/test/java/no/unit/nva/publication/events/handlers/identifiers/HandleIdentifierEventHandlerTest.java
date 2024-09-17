package no.unit.nva.publication.events.handlers.identifiers;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.AdditionalIdentifierBase;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class HandleIdentifierEventHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = new FakeContext();

    public static final String RESOURCE_UPDATE_EVENT_TOPIC = "PublicationService.Resource.Update";
    private static final String RESPONSE_BODY = "{\"handle\": \"https://test.handle.net/123/456\"}";
    public static final int BAD_REQUEST = 400;
    public static final String API_NVATEST_DOMAIN = "api.nvatest.no";
    public static final String HANDLE_BASE_PATH = "handle";
    private ResourceService resourceService;
    private HandleIdentifierEventHandler handler;
    private S3Driver s3Driver;
    private Environment environment;
    private HttpClient httpClient;

    @BeforeEach
    public void setup() throws IOException, InterruptedException {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        var s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, randomString());
        environment = mock(Environment.class);
        when(environment.readEnv("API_DOMAIN")).thenReturn(API_NVATEST_DOMAIN);
        when(environment.readEnv("HANDLE_BASE_PATH")).thenReturn(HANDLE_BASE_PATH);
        when(environment.readEnv("BACKEND_CLIENT_SECRET_NAME")).thenReturn("testSecret");
        when(environment.readEnv("BACKEND_CLIENT_AUTH_URL")).thenReturn("cognitoTestUrl");
        when(environment.readEnv("NVA_FRONTEND_DOMAIN")).thenReturn("nvatest.no");
        httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any())).thenReturn(FakeHttpResponse.create(RESPONSE_BODY, HTTP_CREATED));
        SecretsManagerClient secretManager = mock(SecretsManagerClient.class);
        when(secretManager.getSecretValue((GetSecretValueRequest) any())).thenReturn(
            GetSecretValueResponse.builder()
                .secretString(new BackendClientCredentials("id", "secret").toString())
                .build());
        handler = new HandleIdentifierEventHandler(resourceService, s3Client, environment, httpClient, secretManager);
    }

    @ParameterizedTest
    @EnumSource(
        value = PublicationStatus.class,
        mode = INCLUDE,
        names = {"PUBLISHED", "PUBLISHED_METADATA"})
    void shouldCreateHandlesForPublicationIfMissing(PublicationStatus status)
        throws IOException, BadRequestException, NotFoundException {
        var oldImage = createUnpublishedPublicationWithAdditionalIdentifiers(null);
        var newImage = oldImage.copy().withStatus(status).build();

        var request = emualteSqsWrappedEvent(oldImage, newImage);
        handler.handleRequest(request, CONTEXT);

        var updatedPublication = resourceService.getPublicationByIdentifier(newImage.getIdentifier());
        assertThat(updatedPublication.getAdditionalIdentifiers().size(), is(equalTo(1)));
    }

    private SQSEvent emualteSqsWrappedEvent(Publication oldImage, Publication newImage) throws IOException {
        var blobUri = createSampleBlob(oldImage, newImage);
        var eventBridgeObject =
            EventBridgeEventBuilder.sampleEventObject(AwsEventBridgeDetail.newBuilder()
                                                          .withResponsePayload(
                                                              new EventReference(RESOURCE_UPDATE_EVENT_TOPIC, blobUri))
                                                          .build());
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody(eventBridgeObject.toJsonString());
        sqsEvent.setRecords(List.of(sqsMessage));
        return sqsEvent;
    }

    @ParameterizedTest
    @EnumSource(
        value = PublicationStatus.class,
        mode = INCLUDE,
        names = {"PUBLISHED", "PUBLISHED_METADATA"})
    void shouldNotCreateHandlesForPublicationIfAlreadyExisting(PublicationStatus status)
        throws IOException, BadRequestException, NotFoundException {
        var oldImage = createUnpublishedPublication();
        var additionalIdentifiers = oldImage.getAdditionalIdentifiers();
        var newImage = oldImage.copy().withStatus(status).build();

        var request = emualteSqsWrappedEvent(oldImage, newImage);
        handler.handleRequest(request, CONTEXT);

        var updatedPublication = resourceService.getPublicationByIdentifier(newImage.getIdentifier());
        assertThat(updatedPublication.getAdditionalIdentifiers(), is(equalTo(additionalIdentifiers)));
    }

    @ParameterizedTest
    @EnumSource(
        value = PublicationStatus.class,
        mode = INCLUDE,
        names = {"PUBLISHED", "PUBLISHED_METADATA"})
    void shouldNotCreateHandlesForPublicationIfLegacyHandleAlreadyExist(PublicationStatus status)
        throws IOException, BadRequestException, NotFoundException {
        var oldImage = createUnpublishedPublicationWithAdditionalIdentifiers(
            Set.of(new AdditionalIdentifier(HANDLE_BASE_PATH, "https://test.handle.net/123/456")));
        var additionalIdentifiers = oldImage.getAdditionalIdentifiers();
        var newImage = oldImage.copy().withStatus(status).build();

        var request = emualteSqsWrappedEvent(oldImage, newImage);
        handler.handleRequest(request, CONTEXT);

        var updatedPublication = resourceService.getPublicationByIdentifier(newImage.getIdentifier());
        assertThat(updatedPublication.getAdditionalIdentifiers(), is(equalTo(additionalIdentifiers)));
    }

    @ParameterizedTest
    @EnumSource(
        value = PublicationStatus.class,
        mode = EXCLUDE,
        names = {"PUBLISHED", "PUBLISHED_METADATA"})
    void shouldNotCreateHandlesForUnpublishedPublication(PublicationStatus status)
        throws IOException, BadRequestException, NotFoundException {
        var oldImage = createUnpublishedPublicationWithAdditionalIdentifiers(null);
        var additionalIdentifiers = oldImage.getAdditionalIdentifiers();
        var newImage = oldImage.copy().withStatus(status).build();

        var request = emualteSqsWrappedEvent(oldImage, newImage);
        handler.handleRequest(request, CONTEXT);

        var updatedPublication = resourceService.getPublicationByIdentifier(newImage.getIdentifier());
        assertThat(updatedPublication.getAdditionalIdentifiers(), is(equalTo(additionalIdentifiers)));
    }

//    @Test
//    void shouldThrowOnAPIFailure()
//        throws IOException, BadRequestException, InterruptedException {
//        final var logger = LogUtils.getTestingAppenderForRootLogger();
//        var oldImage = createUnpublishedPublicationWithAdditionalIdentifiers(null);
//        var newImage = oldImage.copy().withStatus(PublicationStatus.PUBLISHED).build();
//        var handleRequestUri = URI.create("https://" + API_NVATEST_DOMAIN + "/" + HANDLE_BASE_PATH);
//        when(httpClient.send(argThat(request -> request.uri().equals(handleRequestUri)),any()))
//            .thenReturn(FakeHttpResponse.create("some error message", BAD_REQUEST));
//
//        var request = emualteSqsWrappedEvent(oldImage, newImage);
//        assertThrows(RuntimeException.class, () -> handler.handleRequest(request, CONTEXT));
//        assertThat(logger.getMessages(), containsString("Error response from server: 400"));
//    }

    private URI createSampleBlob(Object oldImage, Object newImage) throws IOException {
        var oldImageResource = crateDataEntry(oldImage);
        var newImageResource = crateDataEntry(newImage);
        var dataEntryUpdateEvent =
            new DataEntryUpdateEvent(RESOURCE_UPDATE_EVENT_TOPIC, oldImageResource, newImageResource);
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        return s3Driver.insertFile(filePath, dataEntryUpdateEvent.toJsonString());
    }

    private Entity crateDataEntry(Object image) {

        return switch (image) {
            case Publication publication -> Resource.fromPublication(publication);
            case DoiRequest doiRequest -> doiRequest;
            case Message message -> message;
            case null, default -> null;
        };
    }

    private Publication createUnpublishedPublication() throws BadRequestException {
        var publication = randomPublication();
        publication.setStatus(PublicationStatus.UNPUBLISHED);
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }

    private Publication createUnpublishedPublicationWithAdditionalIdentifiers(
        Set<AdditionalIdentifierBase> additionalIdentifiers) throws BadRequestException {
        var publication = randomPublication();
        publication.setStatus(PublicationStatus.UNPUBLISHED);
        publication.setAdditionalIdentifiers(additionalIdentifiers);
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }
}
