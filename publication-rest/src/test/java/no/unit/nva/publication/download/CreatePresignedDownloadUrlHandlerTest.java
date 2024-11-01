package no.unit.nva.publication.download;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseAcceptingFilesForAllTypes;
import static no.unit.nva.publication.CustomerApiStubs.stubSuccessfulTokenResponse;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.testutils.TestHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.associatedartifacts.PublishedFileGenerator;
import no.unit.nva.model.testing.associatedartifacts.UnpublishedFileGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.services.UriShortener;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@WireMockTest(httpsEnabled = true)
class CreatePresignedDownloadUrlHandlerTest extends ResourcesLocalTest {

    private static final String SOME_API_KEY = "some api key";
    private static final String OWNER_USER_ID = "owner@unit.no";
    private static final String NON_OWNER = "non.owner@unit.no";
    private static final String CURATOR = "curator@unit.no";
    private static final String APPLICATION_PROBLEM_JSON = "application/problem+json";
    private static final String PRESIGNED_DOWNLOAD_URL = "https://example.com/download/12345";
    private static final UUID FILE_IDENTIFIER = UUID.randomUUID();
    private static final UUID UNEMBARGOED_FILE_IDENTIFIER = UUID.randomUUID();
    private static final UUID EMBARGOED_FILE_IDENTIFIER = UUID.randomUUID();
    private static final UUID ADMINISTRATIVE_IDENTIFIER = UUID.randomUUID();
    private static final String ANY_ORIGIN = "*";
    private static final String APPLICATION_JSON = "application/json; charset=utf-8";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final URI CUSTOMER = randomUri();
    private Context context;
    private ByteArrayOutputStream output;
    private UriShortener uriShortener;
    private S3Presigner s3Presigner;
    private ResourceService resourceService;
    private Environment environment;
    private HttpClient httpClient;
    private IdentityServiceClient identityServiceClient;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) {
        super.init();
        httpClient = mock(HttpClient.class);
        resourceService = spy(getResourceServiceBuilder().build());

        context = mock(Context.class);
        output = new ByteArrayOutputStream();
        uriShortener = mock(UriShortener.class);
        environment = mockEnvironment(wireMockRuntimeInfo);

        s3Presigner = mock(S3Presigner.class);

        var signedMock = mock(PresignedGetObjectRequest.class);
        when(s3Presigner.presignGetObject((GetObjectPresignRequest) any())).thenReturn(signedMock);//new URL

        identityServiceClient = mock(IdentityServiceClient.class);

        stubSuccessfulTokenResponse();
        stubCustomerResponseAcceptingFilesForAllTypes(CUSTOMER);
    }

    @ParameterizedTest(name = "Should return not found when user is not owner and publication is unpublished")
    @MethodSource("fileTypeSupplier")
    void shouldReturnNotFoundWhenUserIsNotOwnerAndPublicationIsUnpublished(File file) throws IOException {
        var publication = buildPublication(DRAFT, file);
        var handler = getCreatePresignedDownloadUrlHandler();
        var request = createRequest(NON_OWNER, publication.getIdentifier(), file.getIdentifier());
        handler.handleRequest(request, output, context);

        var gatewayResponse = GatewayResponse.fromString(output.toString(), Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_NOT_FOUND, APPLICATION_PROBLEM_JSON);
    }

    private CreatePresignedDownloadUrlHandler getCreatePresignedDownloadUrlHandler() {
        return new CreatePresignedDownloadUrlHandler(resourceService, s3Presigner, environment, uriShortener,
                                                     httpClient, identityServiceClient);
    }

    @ParameterizedTest(
        name = "Files which is not draft but from a type requiring elevated rights is not downloadable by non owner")
    @MethodSource("fileTypeSupplierRequiringElevatedRights")
    void handlerReturnsNotFoundForFilesWhichIsNotDraftButHasTypeRequiringElevationAndIsNonOwner(File file)
        throws IOException {
        var publication = buildPublication(PUBLISHED, file);
        var handler = getCreatePresignedDownloadUrlHandler();
        var request = createRequest(NON_OWNER, publication.getIdentifier(), file.getIdentifier());
        handler.handleRequest(request, output, context);

        var gatewayResponse = GatewayResponse.fromString(output.toString(), Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_NOT_FOUND, APPLICATION_PROBLEM_JSON);
    }

    @Test
    void shouldThrowExceptionWhenEnvironmentVariablesAreUnset() {
        assertThrows(IllegalStateException.class, CreatePresignedDownloadUrlHandler::new);
    }

    @ParameterizedTest(name = "Published publication is downloadable by user {0}")
    @MethodSource("userSupplier")
    void handlerReturnsOkResponseOnValidInputPublishedPublication(String user) throws IOException {
        var publication = buildPublication(PUBLISHED, fileWithoutEmbargo(APPLICATION_PDF, FILE_IDENTIFIER));
        var handler = getCreatePresignedDownloadUrlHandler();
        var request = createRequest(user, publication.getIdentifier(), FILE_IDENTIFIER);
        handler.handleRequest(request, output, context);

        var gatewayResponse = GatewayResponse.fromString(output.toString(), PresignedUriResponse.class);
        assertBasicRestRequirements(gatewayResponse, SC_OK, APPLICATION_JSON);
        assertExpectedResponseBody(gatewayResponse);
    }

    @Test
    void shouldReturnOkWhenPublicationUnpublishedAndUserIsOwner() throws IOException {
        var publication = buildPublication(DRAFT, fileWithoutEmbargo(APPLICATION_PDF, FILE_IDENTIFIER));
        var handler = getCreatePresignedDownloadUrlHandler();
        var request = createRequest(
            publication.getResourceOwner().getOwner().getValue(),
            publication.getIdentifier(),
            FILE_IDENTIFIER);
        handler.handleRequest(request, output, context);

        var gatewayResponse = GatewayResponse.fromString(output.toString(), PresignedUriResponse.class);
        assertBasicRestRequirements(gatewayResponse, SC_OK, APPLICATION_JSON);
        assertExpectedResponseBody(gatewayResponse);
    }

    @Test
    void shouldReturnOkWhenPublicationUnpublishedAndUserHasAccessRightManageResourcesStandard()
        throws IOException {
        var publication = buildPublication(DRAFT, fileWithoutEmbargo(APPLICATION_PDF, FILE_IDENTIFIER));
        var handler = getCreatePresignedDownloadUrlHandler();
        var customer = randomUri();
        handler.handleRequest(createRequestWithAccessRight(
                                  NON_OWNER,
                                  publication.getIdentifier(),
                                  FILE_IDENTIFIER,
                                  customer,
                                  MANAGE_DEGREE_EMBARGO, MANAGE_RESOURCES_STANDARD),
                              output,
                              context);

        var gatewayResponse = GatewayResponse.fromString(output.toString(), PresignedUriResponse.class);
        assertBasicRestRequirements(gatewayResponse, SC_OK, APPLICATION_JSON);
        assertExpectedResponseBody(gatewayResponse);
    }

    @ParameterizedTest(name = "Should return presigned URI when mime-type of file is {0}")
    @MethodSource("mimeTypeProvider")
    void shouldReturnOkWhenPublicationUnpublishedAndUserIsOwnerAndMimeTypeIs(String mimeType)
        throws IOException {
        var publication = buildPublication(DRAFT, fileWithoutEmbargo(APPLICATION_PDF, FILE_IDENTIFIER));
        var handler = getCreatePresignedDownloadUrlHandler();

        handler.handleRequest(createRequest(publication.getResourceOwner().getOwner().getValue(),
                                            publication.getIdentifier(), FILE_IDENTIFIER), output, context);

        var gatewayResponse = GatewayResponse.fromString(output.toString(), PresignedUriResponse.class);
        assertBasicRestRequirements(gatewayResponse, SC_OK, APPLICATION_JSON);
        assertExpectedResponseBody(gatewayResponse);
    }

    // Error message here is odd
    @Test
    void handlerReturnsNotFoundResponseOnUnknownIdentifier()
        throws IOException, nva.commons.apigateway.exceptions.NotFoundException {
        var publication = buildPublication(DRAFT, fileWithoutEmbargo(APPLICATION_PDF, FILE_IDENTIFIER));
        var handler = getCreatePresignedDownloadUrlHandler();
        when(resourceService.getResourceByIdentifier(publication.getIdentifier())).thenThrow(
            new nva.commons.apigateway.exceptions.NotFoundException("test"));

        handler.handleRequest(createRequest(OWNER_USER_ID, publication.getIdentifier(), FILE_IDENTIFIER), output,
                              context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_NOT_FOUND, APPLICATION_PROBLEM_JSON);
    }

    @ParameterizedTest(name = "Unpublished publication with filetype {1} is downloadable by user {0}")
    @MethodSource("userFileTypeSupplier")
    void handlerReturnsOkResponseOnValidInputPublication(String user, File file)
        throws IOException {

        var publication = buildPublication(DRAFT, file);
        var handler = getCreatePresignedDownloadUrlHandler();

        handler.handleRequest(
            createRequestWithAccessRight(user, publication.getIdentifier(), file.getIdentifier(), CUSTOMER,
                                         MANAGE_DEGREE_EMBARGO, MANAGE_RESOURCES_STANDARD),
            output, context);

        var gatewayResponse = GatewayResponse.fromString(output.toString(), PresignedUriResponse.class);
        assertBasicRestRequirements(gatewayResponse, SC_OK, APPLICATION_JSON);
        assertExpectedResponseBody(gatewayResponse);
    }

    @Test
    void handlerReturnsNotFoundOnPublicationWithoutFile() throws IOException,  ApiGatewayException {
        var publication = buildPublishedPublicationWithoutFiles();
        var fileIdentifier = UUID.randomUUID();
        var handler = spy(getCreatePresignedDownloadUrlHandler());
        doAnswer((Answer<Void>) invocation -> {
            RequestInfo mockRequestInfo = mock(RequestInfo.class);
            var arg2 = invocation.getArgument(2, Context.class);
            invocation.callRealMethod();
            handler.processInput(null, mockRequestInfo, arg2);
            return null;
        }).when(handler).processInput(any(), any(), any());

        handler.handleRequest(createRequest(OWNER_USER_ID, publication.getIdentifier(), fileIdentifier),
                              output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_NOT_FOUND, APPLICATION_PROBLEM_JSON);
    }

    @Test
    void shouldReturnServiceUnavailableResponseOnS3ServiceException() throws IOException {
        var publication = buildPublication(PUBLISHED, fileWithoutEmbargo(APPLICATION_PDF, FILE_IDENTIFIER));
        var publicationIdentifier = publication.getIdentifier();
        when(s3Presigner.presignGetObject((GetObjectPresignRequest) any())).thenThrow(new SdkClientException("test"));
        var handler = getCreatePresignedDownloadUrlHandler();

        handler.handleRequest(
            createRequest(
                publication.getResourceOwner().getOwner().getValue(),
                publicationIdentifier,
                FILE_IDENTIFIER),
            output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_BAD_GATEWAY, APPLICATION_PROBLEM_JSON);
    }

    @Test
    void shouldReturnNotFoundOnAnonymousRequestForDraftPublication()
        throws IOException {
        var publication = buildPublication(DRAFT, fileWithoutEmbargo(APPLICATION_PDF, FILE_IDENTIFIER));
        var handler = getCreatePresignedDownloadUrlHandler();
        handler.handleRequest(createAnonymousRequest(publication.getIdentifier()), output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_NOT_FOUND, APPLICATION_PROBLEM_JSON);
    }

    @ParameterizedTest(name = "Should return Not Found when requester is not owner and embargo is in place")
    @MethodSource("nonOwnerProvider")
    void shouldDisallowDownloadByNonOwnerWhenEmbargoDateHasNotPassed(InputStream request) throws IOException,
                                                                                                 InterruptedException {
        var publication = buildPublication(PUBLISHED, fileWithEmbargo(FILE_IDENTIFIER));
        mockSuccessfulPublicationRequest(dtoObjectMapper.writeValueAsString(publication));
        var handler = getCreatePresignedDownloadUrlHandler();
        handler.handleRequest(request, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_NOT_FOUND, APPLICATION_PROBLEM_JSON);
    }

    @ParameterizedTest(name = "Should return Not Found when requester is not owner and file is administrative "
                              + "agreement")
    @MethodSource("nonOwnerProvider")
    void shouldDisallowDownloadByNonOwnerWhenFileHasIsAdministrativeAgreement(InputStream request)
        throws IOException, InterruptedException {
        var publication = buildPublication(PUBLISHED, fileWithTypeUnpublishable(FILE_IDENTIFIER, true));
        mockSuccessfulPublicationRequest(dtoObjectMapper.writeValueAsString(publication));
        var handler = getCreatePresignedDownloadUrlHandler();

        handler.handleRequest(request, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_NOT_FOUND, APPLICATION_PROBLEM_JSON);
    }

    @Test
    void shouldThrowInternalServerExceptionIfUriShortenerFails() throws IOException, InterruptedException {
        var publication = buildPublication(DRAFT, fileWithoutEmbargo(APPLICATION_PDF, FILE_IDENTIFIER));
        mockSuccessfulPublicationRequest(dtoObjectMapper.writeValueAsString(publication));
        when(uriShortener.shorten(any(), any())).thenThrow(new RuntimeException("shouldThrowInternalServerExceptionIfUriShortenerFails"));
        var handler = getCreatePresignedDownloadUrlHandler();
        var customer = randomUri();
        handler.handleRequest(createRequestWithAccessRight(
                                  NON_OWNER,
                                  publication.getIdentifier(),
                                  FILE_IDENTIFIER,
                                  customer,
                                  MANAGE_DEGREE_EMBARGO, MANAGE_RESOURCES_STANDARD),
                              output,
                              context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertBasicRestRequirements(gatewayResponse, SC_INTERNAL_SERVER_ERROR, APPLICATION_PROBLEM_JSON);
    }

    private static Stream<String> userSupplier() {
        return Stream.of(
            OWNER_USER_ID,
            null,
            NON_OWNER,
            CURATOR
        );
    }

    private static Stream<Arguments> userFileTypeSupplier() {
        return Stream.of(
            Arguments.of(OWNER_USER_ID, fileWithEmbargo(EMBARGOED_FILE_IDENTIFIER)),
            Arguments.of(OWNER_USER_ID, fileWithoutEmbargo(APPLICATION_PDF, UNEMBARGOED_FILE_IDENTIFIER)),
            Arguments.of(OWNER_USER_ID, fileWithTypeUnpublishable(ADMINISTRATIVE_IDENTIFIER, true)),
            Arguments.of(OWNER_USER_ID, fileWithTypeUnpublishable(ADMINISTRATIVE_IDENTIFIER, false)),
            Arguments.of(OWNER_USER_ID, fileWithTypeUnpublished()),
            Arguments.of(CURATOR, fileWithEmbargo(EMBARGOED_FILE_IDENTIFIER)),
            Arguments.of(CURATOR, fileWithoutEmbargo(APPLICATION_PDF, UNEMBARGOED_FILE_IDENTIFIER)),
            Arguments.of(CURATOR, fileWithTypeUnpublishable(ADMINISTRATIVE_IDENTIFIER, true)),
            Arguments.of(CURATOR, fileWithTypeUnpublishable(ADMINISTRATIVE_IDENTIFIER, false)),
            Arguments.of(CURATOR, fileWithTypeUnpublished())
        );
    }

    private static Stream<String> mimeTypeProvider() {
        return Stream.of(
            APPLICATION_PDF,
            null
        );
    }

    private static Stream<InputStream> nonOwnerProvider() throws IOException {
        return Stream.of(
            createAnonymousRequest(SortableIdentifier.next()),
            createNonOwnerRequest(SortableIdentifier.next())
        );
    }

    private static Stream<File> fileTypeSupplier() {
        return Stream.of(
            fileWithEmbargo(EMBARGOED_FILE_IDENTIFIER),
            fileWithoutEmbargo(APPLICATION_PDF, UNEMBARGOED_FILE_IDENTIFIER),
            fileWithTypeUnpublishable(ADMINISTRATIVE_IDENTIFIER, true),
            fileWithTypeUnpublishable(ADMINISTRATIVE_IDENTIFIER, false),
            fileWithTypeUnpublished()
        );
    }

    private static Stream<File> fileTypeSupplierRequiringElevatedRights() {
        return Stream.of(
            fileWithEmbargo(EMBARGOED_FILE_IDENTIFIER),
            fileWithTypeUnpublishable(ADMINISTRATIVE_IDENTIFIER, true),
            fileWithTypeUnpublishable(ADMINISTRATIVE_IDENTIFIER, false),
            fileWithTypeUnpublished()
        );
    }

    private static File fileWithEmbargo(UUID fileIdentifier) {
        var embargo = Instant.now().plus(Duration.ofDays(3L));
        return PublishedFileGenerator.random()
                   .copy()
                   .withIdentifier(fileIdentifier)
                   .withMimeType(APPLICATION_PDF)
                   .withEmbargoDate(embargo)
                   .buildPublishedFile();
    }

    private static File fileWithoutEmbargo(String mimeType, UUID fileIdentifier) {
        return PublishedFileGenerator.random()
                   .copy()
                   .withIdentifier(fileIdentifier)
                   .withMimeType(mimeType)
                   .buildPublishedFile();
    }

    private static File fileWithTypeUnpublished() {
        return UnpublishedFileGenerator.random()
                   .copy()
                   .withIdentifier(FILE_IDENTIFIER)
                   .withMimeType(APPLICATION_PDF)
                   .buildUnpublishedFile();
    }

    private static File fileWithTypeUnpublishable(UUID fileIdentifier, boolean administrativeAgreement) {
        return UnpublishedFileGenerator.random()
                   .copy()
                   .withIdentifier(fileIdentifier)
                   .withMimeType(APPLICATION_PDF)
                   .withAdministrativeAgreement(administrativeAgreement)
                   .buildUnpublishableFile();
    }

    private static InputStream createAnonymousRequest(SortableIdentifier publicationIdentifier) throws
                                                                                                IOException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withPathParameters(Map.of(RequestUtil.PUBLICATION_IDENTIFIER, publicationIdentifier.toString(),
                                              RequestUtil.FILE_IDENTIFIER, FILE_IDENTIFIER.toString()))
                   .build();
    }

    private static InputStream createNonOwnerRequest(SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withUserName(NON_OWNER)
                   .withCurrentCustomer(randomUri())
                   .withPathParameters(Map.of(RequestUtil.PUBLICATION_IDENTIFIER, publicationIdentifier.toString(),
                                              RequestUtil.FILE_IDENTIFIER, FILE_IDENTIFIER.toString()))
                   .build();
    }

    private Publication buildPublication(PublicationStatus status, File file) {

        var publicationInstanceType = !file.fileDoesNotHaveActiveEmbargo() ? DegreeMaster.class :
                                                                                                    AcademicMonograph.class;
        var randomPublication = PublicationGenerator.randomPublication(publicationInstanceType);
        randomPublication.setAssociatedArtifacts(new AssociatedArtifactList(file));
        randomPublication.setStatus(status);
        randomPublication.setResourceOwner(new ResourceOwner(new Username(OWNER_USER_ID), CUSTOMER));
        var user = UserInstance.createExternalUser(randomPublication.getResourceOwner(), randomUri());

        var publication = attempt(
            () -> Resource.fromPublication(randomPublication)
                      .persistNew(resourceService, user)).orElseThrow();

        mockS3Presigner(s3Presigner, file);

        return  publication;
    }

    private Publication buildPublishedPublicationWithoutFiles() {
        var publication = buildPublication(PUBLISHED, fileWithTypeUnpublished());

        var nullAssociatedArtifact = new NullAssociatedArtifact();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(nullAssociatedArtifact));

        return publication;
    }

    private void assertBasicRestRequirements(GatewayResponse<?> gatewayResponse,
                                             int expectedStatusCode,
                                             String expectedContentType) {
        assertThat(gatewayResponse.getStatusCode(), equalTo(expectedStatusCode));
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders().get(CONTENT_TYPE), equalTo(expectedContentType));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(gatewayResponse.getHeaders().get(ACCESS_CONTROL_ALLOW_ORIGIN), equalTo(ANY_ORIGIN));
    }

    private void assertExpectedResponseBody(GatewayResponse<PresignedUriResponse> gatewayResponse)
        throws JsonProcessingException {
        var body = gatewayResponse.getBodyObject(PresignedUriResponse.class);
        assertThat(body.signedUri(), is(notNullValue()));
        assertTrue(greaterThanNow(body.expires()));
    }

    private boolean greaterThanNow(Instant instant) {
        return Instant.now().isBefore(instant);
    }

    private Environment mockEnvironment(WireMockRuntimeInfo wireMockRuntimeInfo) {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(ANY_ORIGIN);
        var baseUrl = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        when(environment.readEnv("BACKEND_CLIENT_AUTH_URL"))
            .thenReturn(baseUrl.toString());
        return environment;
    }

    private void mockSuccessfulPublicationRequest(String responseBody)
        throws IOException, InterruptedException {
        /*var publicationService = new RestPublicationService(httpClient, dtoObjectMapper, API_SCHEME, API_HOST);
        @SuppressWarnings("unchecked")
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when((response.body())).thenAnswer(i -> responseBody);
        when(httpClient.<String>send(any(), any())).thenAnswer((Answer<HttpResponse<String>>) invocation -> response);
        return publicationService;*/
    }

    private InputStream createRequest(String user, SortableIdentifier identifier, UUID fileIdentifier)
        throws IOException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withHeaders(Map.of(AUTHORIZATION, SOME_API_KEY))
                   .withCurrentCustomer(randomUri())
                   .withUserName(user)
                   .withPathParameters(Map.of(RequestUtil.PUBLICATION_IDENTIFIER, identifier.toString(),
                                              RequestUtil.FILE_IDENTIFIER, fileIdentifier.toString()))
                   .build();
    }

    private InputStream createRequestWithAccessRight(String user,
                                                     SortableIdentifier identifier,
                                                     UUID fileIdentifier,
                                                     URI customer,
                                                     AccessRight... accessRight) throws IOException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withHeaders(Map.of(AUTHORIZATION, SOME_API_KEY))
                   .withCurrentCustomer(customer)
                   .withAccessRights(customer, accessRight)
                   .withUserName(user)
                   .withPathParameters(Map.of(RequestUtil.PUBLICATION_IDENTIFIER, identifier.toString(),
                                              RequestUtil.FILE_IDENTIFIER, fileIdentifier.toString()))
                   .build();
    }

    private void mockS3Presigner(S3Presigner s3Presigner, File file) {
        var request = mock(PresignedGetObjectRequest.class);
        var presignedUrl = attempt(() -> URI.create(PRESIGNED_DOWNLOAD_URL).toURL()).orElseThrow();
        when(request.url()).thenReturn(presignedUrl);
        var expires = Instant.now();
        when(request.expiration()).thenReturn(expires);
        when(s3Presigner.presignGetObject((GetObjectPresignRequest) any())).thenReturn(request);
        PresignedUri.builder()
            .withFileIdentifier(file.getIdentifier())
            .withSignedUri(attempt(presignedUrl::toURI).orElseThrow())
            .withExpiration(expires)
            .build();
    }
}
