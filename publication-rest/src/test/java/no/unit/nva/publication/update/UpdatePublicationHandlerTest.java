package no.unit.nva.publication.update;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationOperation.DELETE;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.testing.PublicationGenerator.fromInstanceClassesExcluding;
import static no.unit.nva.model.testing.PublicationGenerator.randomEntityDescription;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomUploadedFile;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseAcceptingFilesForAllTypes;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseNotFound;
import static no.unit.nva.publication.CustomerApiStubs.stubSuccessfulCustomerResponseAllowingFilesForNoTypes;
import static no.unit.nva.publication.CustomerApiStubs.stubSuccessfulTokenResponse;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.RequestUtil.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.delete.DeletePublicationHandler.LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS;
import static no.unit.nva.publication.delete.DeletePublicationHandler.NVA_PUBLICATION_DELETE_SOURCE;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.model.business.TicketStatus.NOT_APPLICABLE;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.SCOPE_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_NVI_CANDIDATES;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_RESOURCES;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.ConferenceAbstract;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.delete.LambdaDestinationInvocationDetail;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.http.RandomPersonServiceResponse;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.http.entity.ContentType;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

@WireMockTest(httpsEnabled = true)
class UpdatePublicationHandlerTest extends ResourcesLocalTest {

    private static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE =
        restApiMapper.getTypeFactory().constructParametricType(GatewayResponse.class, Problem.class);
    private static final String SOME_MESSAGE = "SomeMessage";
    private static final String SOME_CURATOR = "some@curator";
    private static final String SOME_CONTRIBUTOR = "contributor@org";
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    public static final String NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    public static final String EVENT_BUS_NAME = "EVENT_BUS_NAME";
    public static final String UNPUBLISH_REQUEST_REQUIRES_A_COMMENT = "Unpublish request requires a comment";
    private static final String API_HOST_KEY = "API_HOST";
    private static final String API_HOST_DOMAIN = "localhost";
    public static final String PUBLICATION = "publication";
    public static final String MUST_BE_A_VALID_PUBLICATION_API_URI = "must be a valid publication API URI";
    public static final String COMMENT_ON_UNPUBLISHING_REQUEST = "comment";
    public static final String BACKEND_SCOPE = "https://api.nva.unit.no/scopes/backend";
    private static final String SCOPES_THIRD_PARTY_PUBLICATION_READ = "https://api.nva.unit.no/scopes/third-party/publication-read";

    private final GetExternalClientResponse getExternalClientResponse = mock(GetExternalClientResponse.class);
    final Context context = new FakeContext();
    ResourceService resourceService;
    ByteArrayOutputStream output;
    protected UpdatePublicationHandler updatePublicationHandler;
    Publication publication;
    private Environment environment;
    private IdentityServiceClient identityServiceClient;
    private TicketService ticketService;
    private FakeSecretsManagerClient secretsManagerClient;
    private FakeEventBridgeClient eventBridgeClient;
    private URI customerId;

    public static Stream<Named<AccessRight[]>> privilegedUserProvider() {
        return Stream.of(
            Named.of("Editor", new AccessRight[]{MANAGE_RESOURCES_ALL}),
            Named.of("Curator", new AccessRight[]{MANAGE_RESOURCES_STANDARD, MANAGE_PUBLISHING_REQUESTS})
        );
    }

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) throws NotFoundException {
        super.init();

        environment = mock(Environment.class);
        uriRetriever = mock(UriRetriever.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv(NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY)).thenReturn(
            NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY);
        when(environment.readEnv(API_HOST_KEY)).thenReturn("localhost");
        when(environment.readEnv("COGNITO_AUTHORIZER_URLS")).thenReturn("http://localhost:3000");
        lenient().when(environment.readEnv("BACKEND_CLIENT_SECRET_NAME")).thenReturn("secret");
        var baseUrl = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        lenient().when(environment.readEnv("BACKEND_CLIENT_AUTH_URL"))
            .thenReturn(baseUrl.toString());

        resourceService = getResourceServiceBuilder().build();
        this.ticketService = getTicketService();

        this.eventBridgeClient = new FakeEventBridgeClient(EVENT_BUS_NAME);

        identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);

        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("secret", credentials.toString());
        output = new ByteArrayOutputStream();
        var httpClient = WiremockHttpClient.create();
        updatePublicationHandler =
            new UpdatePublicationHandler(resourceService, ticketService, environment, identityServiceClient,
                                         eventBridgeClient, secretsManagerClient, httpClient);

        customerId = UriWrapper.fromUri(wireMockRuntimeInfo.getHttpsBaseUrl())
                         .addChild("customer", randomUUID().toString())
                         .getUri();

        publication = randomPublicationWithPublisher();

        stubSuccessfulTokenResponse();
        stubCustomerResponseAcceptingFilesForAllTypes(customerId);
    }

    private Publication randomPublicationWithPublisher() {
        return randomPublication()
                   .copy()
                   .withPublisher(new Organization.Builder()
                                      .withId(customerId)
                                      .build())
                   .build();
    }

    private static Publication randomPublicationWithPublisher(URI publisherId, Class<?> publicationInstanceClass) {
        return randomPublication(publicationInstanceClass)
                   .copy()
                   .withPublisher(new Organization.Builder()
                                      .withId(publisherId)
                                      .build())
                   .build();
    }

    private Publication randomNonDegreePublicationWithPublisher() {
        return fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES)
                   .copy()
                   .withPublisher(new Organization.Builder()
                                      .withId(customerId)
                                      .build())
                   .build();
    }

    static Stream<Arguments> allProtectedDegreeInstances() {
        return Stream.of(
            Arguments.of(DegreeLicentiate.class),
            Arguments.of(DegreeMaster.class),
            Arguments.of(DegreeBachelor.class),
            Arguments.of(DegreePhd.class)
        );
    }

    @Test
    void handlerUpdatesPublicationWhenInputIsValidAndUserIsResourceOwner()
        throws IOException, ApiGatewayException {
        publication = publicationWithoutIdentifier(customerId);
        var savedPublication = createSamplePublication();

        var publicationUpdate = updateTitle(savedPublication);

        var event = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        final var body = gatewayResponse.getBodyObject(PublicationResponseElevatedUser.class);
        assertThat(body.getEntityDescription().getMainTitle(),
                   is(equalTo(publicationUpdate.getEntityDescription().getMainTitle())));
    }

    @Test
    void handlerCreatesPendingPublishingRequestTicketForPublishedPublicationWhenUpdatingFilesAndUserIsNotAllowedToAutoPublishFiles()
        throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedNonDegreePublication(customerId,
                                                                                       PUBLISHED,
                                                                                       resourceService);

        var publicationUpdate = addAnotherUploadedFile(publishedPublication);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(customerId);
        updatePublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        final var ticket = ticketService.fetchTicketByResourceIdentifier(publicationUpdate.getPublisher().getId(),
                                                                         publicationUpdate.getIdentifier(),
                                                                         PublishingRequestCase.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(ticket.map(PublishingRequestCase::getStatus).orElseThrow(), is(equalTo(PENDING)));
    }

    @Test
    void handlerCreatesApprovedPublishingRequestTicketForPublishedPublicationWhenUpdatingFilesAndInstitutionAllowsUpdatingFiles()
        throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedNonDegreePublication(customerId,
                                                                                       PUBLISHED,
                                                                                       resourceService);

        var publicationUpdate = addAnotherUploadedFile(publishedPublication);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        final var ticket = ticketService.fetchTicketByResourceIdentifier(publicationUpdate.getPublisher().getId(),
                                                                         publicationUpdate.getIdentifier(),
                                                                         PublishingRequestCase.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(ticket.map(PublishingRequestCase::getStatus).orElseThrow(),
                   is(equalTo(COMPLETED)));
    }

    @Test
    void shouldPersistApprovedPublishingRequestWhenUserHasPublishingAccessRight()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithOpenFiles(customerId,
                                                                                  PublicationStatus.PUBLISHED,
                                                                                  resourceService);

        var existingTicket = TicketTestUtils.createCompletedTicket(publication, PublishingRequestCase.class,
                                                                   ticketService);
        var publicationUpdate = addAnotherUploadedFile(publication);

        var input = curatorPublicationOwnerUpdatesPublication(publicationUpdate);
        updatePublicationHandler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));

        var autoCompletedTicket = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                                      .filter(PublishingRequestCase.class::isInstance)
                                      .map(PublishingRequestCase.class::cast)
                                      .filter(ticket -> !ticket.equals(existingTicket))
                                      .toList().getFirst();
        assertThat(autoCompletedTicket.getStatus(), is(equalTo(COMPLETED)));
    }

    private Publication addAnotherUploadedFile(Publication publication) {
        var file = randomUploadedFile();
        return addFileToPublication(publication, file);
    }

    private Publication addFileToPublication(Publication savedPublication, File file) {
        FileEntry.create(file, savedPublication.getIdentifier(), UserInstance.fromPublication(savedPublication))
            .persist(resourceService);
        var artifacts = new AssociatedArtifactList(new ArrayList<>(savedPublication.getAssociatedArtifacts()));
        artifacts.add(file.copy().withLicense(randomUri()).buildPendingOpenFile());
        return savedPublication.copy().withAssociatedArtifacts(artifacts).build();
    }

    @Test
    void handlerCreatesPendingPublishingRequestTicketForPublishedPublicationWhenCompletedPublishingRequestExists()
        throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(customerId,
                                                                              PUBLISHED,
                                                                              resourceService);
        persistCompletedPublishingRequest(publishedPublication);
        var publicationUpdate = addAnotherUploadedFile(publishedPublication);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(customerId);
        updatePublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        final var tickets = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publishedPublication))
                                .toList();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertTrue(containsOneCompletedAndOnePendingPublishingRequest(tickets));
    }

    @Test
    void handlerDoesNotCreateNewPublishingRequestWhenThereExistsPendingPublishingRequest()
        throws IOException, ApiGatewayException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(customerId,
                                                                              PUBLISHED,
                                                                              resourceService);
        var pendingTicket = createPendingPublishingRequest(publishedPublication);
        var publicationUpdate = addAnotherUploadedFile(publishedPublication);
        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);

        var existingTickets = ticketService.fetchTicketsForUser(UserInstance.fromTicket(pendingTicket)).toList();
        assertThat(existingTickets, hasSize(1));
    }

    @Test
    void handlerDoesNotCreateNewPublishingRequestWhenThereExistsPendingAndCompletedPublishingRequest()
        throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(customerId,
                                                                              PUBLISHED,
                                                                              resourceService);
        persistCompletedPublishingRequest(publishedPublication);
        var pendingPublishingRequest = createPendingPublishingRequest(publishedPublication);
        var publicationUpdate = addAnotherUploadedFile(publishedPublication);
        var inputStream = ownerUpdatesOwnPublication(publishedPublication.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);

        var existingTickets = ticketService.fetchTicketsForUser(UserInstance.fromTicket(pendingPublishingRequest))
                                  .toList();
        assertThat(existingTickets, hasSize(2));
    }

    @Test
    void handlerUpdatesPublicationWhenInputIsValidAndUserIsExternalClient() throws IOException, BadRequestException {
        publication.setIdentifier(null);
        var savedPublication = createSamplePublication();
        var publicationUpdate = updateTitle(savedPublication);

        when(getExternalClientResponse.getCustomerUri())
            .thenReturn(publication.getPublisher().getId());
        when(getExternalClientResponse.getActingUser())
            .thenReturn(publication.getResourceOwner().getOwner().getValue());

        var event = externalClientUpdatesPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        final PublicationResponse body = gatewayResponse.getBodyObject(PublicationResponseElevatedUser.class);
        assertThat(body.getEntityDescription().getMainTitle(),
                   is(equalTo(publicationUpdate.getEntityDescription().getMainTitle())));
    }

    @Test
    void handlerUpdatesPublicationWhenInputIsValidAndUserIsBackendClient() throws IOException, BadRequestException {
        publication.setIdentifier(null);
        var savedPublication = createSamplePublication();
        var publicationUpdate = updateTitle(savedPublication);

        when(getExternalClientResponse.getCustomerUri())
            .thenReturn(randomUri());
        when(getExternalClientResponse.getActingUser())
            .thenReturn(randomString());

        var event = backendClientUpdatesPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        final PublicationResponse body = gatewayResponse.getBodyObject(PublicationResponseElevatedUser.class);
        assertThat(body.getEntityDescription().getMainTitle(),
                   is(equalTo(publicationUpdate.getEntityDescription().getMainTitle())));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        var event = generateInputStreamMissingPathParameters().build();
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(IDENTIFIER_IS_NOT_A_VALID_UUID));
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        resourceService = serviceFailsOnModifyRequestWithRuntimeError();

        updatePublicationHandler = new UpdatePublicationHandler(resourceService,
                                                                ticketService,
                                                                environment,
                                                                identityServiceClient,
                                                                eventBridgeClient,
                                                                secretsManagerClient,
                                                                WiremockHttpClient.create());

        var savedPublication = createSamplePublication();
        var event = ownerUpdatesOwnPublication(savedPublication.getIdentifier(), savedPublication);
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("handler logs error details on unexpected exception")
    void handlerLogsErrorDetailsOnUnexpectedException()
        throws IOException, ApiGatewayException {
        final TestAppender appender = createAppenderForLogMonitoring();
        resourceService = serviceFailsOnModifyRequestWithRuntimeError();
        updatePublicationHandler = new UpdatePublicationHandler(resourceService,
                                                                ticketService,
                                                                environment,
                                                                identityServiceClient,
                                                                eventBridgeClient,
                                                                secretsManagerClient,
                                                                WiremockHttpClient.create());

        var savedPublication = createSamplePublication();
        var event = ownerUpdatesOwnPublication(savedPublication.getIdentifier(), savedPublication, randomUri());
        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = toGatewayResponseProblem();
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(SC_INTERNAL_SERVER_ERROR)));
        assertThat(appender.getMessages(), containsString(SOME_MESSAGE));
    }

    @Test
    void handlerReturnsBadRequestWhenIdentifierInPathDiffersFromIdentifierInBody()
        throws IOException {
        var randpomPersistedPublication = buildPublication(p -> p
                                                                    .map(a -> setOwnerFromPublication(a, publication))
                                                                    .map(this::persistPublication));
        var event = ownerUpdatesOwnPublication(randpomPersistedPublication.getIdentifier(), publication);

        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        var problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(UpdatePublicationHandler.IDENTIFIER_MISMATCH_ERROR_MESSAGE));
    }

    private Builder persistPublication(Builder publicationBuilder) {
        try {
            return Resource
                       .fromPublication(publicationBuilder.build())
                       .persistNew(resourceService, UserInstance.fromPublication(publication))
                       .copy();
        } catch (BadRequestException e) {
            throw new RuntimeException(e);
        }
    }

    private Publication buildPublication(Function<Stream<Publication.Builder>, Stream<Publication.Builder>> p) {
        return p.apply(Stream.of(randomPublication().copy())).map(Builder::build).findFirst().orElseThrow();
    }

    private static Builder setOwnerFromPublication(Publication.Builder builder, Publication publication) {
        return builder
                   .withResourceOwner(publication.getResourceOwner())
                   .withPublisher(publication.getPublisher());
    }

    @Test
    @DisplayName("Handler returns NotFound response when resource does not exist")
    void handlerReturnsNotFoundResponseWhenResourceDoesNotExist() throws IOException {
        var event = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = toGatewayResponseProblem();
        assertEquals(HTTP_NOT_FOUND, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), is(equalTo(RESOURCE_NOT_FOUND_MESSAGE)));
    }

    @Test
    void handlerUpdatesResourceWhenInputIsValidAndUserHasRightToEditAnyResourceInOwnInstitution()
        throws ApiGatewayException, IOException {
        var savedPublication = createSamplePublication();
        var publicationUpdate = updateTitle(savedPublication);
        var event = userUpdatesPublicationAndHasRightToUpdate(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = resourceService.getPublicationByIdentifier(savedPublication.getIdentifier());

        //inject modified date to the input object because modified date is not available before the actual update.
        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        var expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        var actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));

        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @Test
    void handlerThrowsExceptionWhenInputIsValidUserHasRightToEditAnyResourceInOwnInstButEditsResourceInOtherInst()
        throws IOException, BadRequestException {
        var savedPublication = createSamplePublication();
        var publicationUpdate = updateTitle(savedPublication);
        var event = userUpdatesPublicationOfOtherInstitution(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
        assertThat(problem.getDetail(),
                   is(startsWith("Unauthorized: some@curator is not allowed to perform UPDATE on ")));
    }

    @Test
    void handlerReturnsForbiddenWhenExternalClientTriesToUpdateResourcesCreatedByOthers()
        throws IOException, BadRequestException {
        var savedPublication = createSamplePublication();
        var publicationUpdate = updateTitle(savedPublication);

        when(getExternalClientResponse.getCustomerUri()).thenReturn(randomUri());
        when(getExternalClientResponse.getActingUser()).thenReturn(randomString());

        var inputStream = externalClientUpdatesPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserCannotBeIdentified() throws IOException, BadRequestException {
        var savedPublication = createSamplePublication();
        var event = requestWithoutUsername(savedPublication);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldUpdateResourceWhenAuthorizedUserIsContributorAndHasCristinId()
        throws BadRequestException, IOException, NotFoundException {
        var savedPublication = createAndPersistNonDegreePublication();
        var contributors = new ArrayList<>(savedPublication.getEntityDescription().getContributors());
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        contributors.add(contributor);
        contributors.addAll(getRandomContributorsWithoutCristinIdAndIdentity());
        contributors.forEach(c -> injectContributor(savedPublication, c));
        var publicationUpdate = updateTitle(savedPublication);

        var event = contributorUpdatesPublicationAndHasRightsToUpdate(publicationUpdate, cristinId);
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = resourceService.getPublicationByIdentifier(savedPublication.getIdentifier());

        var expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        var actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
    }

    @Test
    void shouldReturnNotFoundWhenContributorUpdatesResourceThatDoesNotExist()
        throws BadRequestException, IOException {
        var savedPublication = createSamplePublication();
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        injectContributor(savedPublication, contributor);
        var nonExistentPublication = savedPublication.copy().withIdentifier(SortableIdentifier.next()).build();
        var event = contributorUpdatesPublicationAndHasRightsToUpdate(nonExistentPublication, cristinId);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnForbiddenWhenContributorWithoutCristinIdUpdatesResource()
        throws BadRequestException, IOException {
        var savedPublication = createSamplePublication();
        var contributor = createContributorForPublicationUpdate(null);
        injectContributor(savedPublication, contributor);
        var publicationUpdate = updateTitle(savedPublication);
        var event = contributorUpdatesPublicationWithoutHavingRights(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldNotReturnHiddenFilesWhenUserOnlyHaveOwnerRights() throws IOException, BadRequestException {
        var resource = Resource
                           .fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        var updatedPublication = updateTitle(resource);
        var input = ownerUpdatesOwnPublication(updatedPublication.getIdentifier(), updatedPublication);
        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HTTP_OK)));

        var artifacts = response.getBodyObject(PublicationResponseElevatedUser.class)
                            .getAssociatedArtifacts()
                            .stream().toList();
        assertFalse(artifacts.isEmpty());
        assertFalse(artifacts.stream()
                        .anyMatch(HiddenFile.class::isInstance));
    }

    @Test
    @DisplayName("Handler returns OK when thesis and is owner")
    void shouldReturnOKWhenUserIsOwner() throws IOException, BadRequestException {
        var thesisPublication = publication.copy().withEntityDescription(thesisPublishableEntityDescription()).build();
        var savedThesis = Resource
                              .fromPublication(thesisPublication)
                              .persistNew(resourceService, UserInstance.fromPublication(publication));
        var updatedPublication = updateTitle(savedThesis);
        var input = ownerUpdatesOwnPublication(updatedPublication.getIdentifier(), updatedPublication);
        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HTTP_OK)));
    }

    @Test
    @DisplayName("Handler returns OK when thesis and user has PUBLISH_THESIS")
    void shouldReturnOKWhenUserHasPublishThesis() throws IOException, BadRequestException {
        var thesisPublication = publication.copy().withEntityDescription(thesisPublishableEntityDescription()).build();
        var savedThesis = Resource
                              .fromPublication(thesisPublication)
                              .persistNew(resourceService, UserInstance.fromPublication(publication));
        var updatedPublication = updateTitle(savedThesis);
        var input = userUpdatesPublicationAndHasRightToUpdate(updatedPublication);
        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HTTP_OK)));
    }

    @Test
    void shouldReturnBadGatewayWhenHttpClientUnableToRetrievePublishingWorkflow()
        throws IOException, ApiGatewayException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(customerId,
                                                                              PUBLISHED,
                                                                              resourceService);

        final var publicationUpdate = addAnotherUploadedFile(publishedPublication);

        WireMock.reset();

        stubSuccessfulTokenResponse();
        stubCustomerResponseNotFound(customerId);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(problem.getDetail(), is(equalTo("Customer API not responding or not responding as expected!")));
    }

    @Test
    void shouldUpdatePublicationWhenUserIsCuratorAndIsInSameInstitutionAsThePublicationContributor()
        throws IOException, NotFoundException {
        var nonDegreePublication = createNonDegreePublication().copy();
        var contributors = new ArrayList<Contributor>();
        var cristinId = randomUri();
        var entityDescription = nonDegreePublication.build().getEntityDescription();
        var contributor = createContributorForPublicationUpdate(cristinId);
        contributors.add(contributor);
        contributors.addAll(getRandomContributorsWithoutCristinIdAndIdentity());
        entityDescription.setContributors(contributors);
        var savedPublication =
            persistPublication(nonDegreePublication
                                   .withEntityDescription(entityDescription)
                                   .withCuratingInstitutions(mockCuratingInstitutions(contributors))).build();
        var customerId = ((Organization) contributor.getAffiliations().getFirst()).getId();
        var topLevelCristinOrgId = ((Organization) contributor.getAffiliations().getFirst()).getId();
        when(uriRetriever.getRawContent(eq(topLevelCristinOrgId), any())).thenReturn(
            Optional.of(String.format("""
                                          {
                                            "@context" : "https://bibsysdev.github.io/src/organization-context.json",
                                            "type" : "Organization",
                                            "id" : "%s",
                                            "acronym" : "SIKT",
                                            "country" : "NO",
                                            "partOf" : [ ],
                                            "hasPart" : [ ]
                                          }""", topLevelCristinOrgId.toString())));

        var publicationUpdate = updateTitle(savedPublication);
        var event = curatorWithAccessRightsUpdatesPublication(publicationUpdate, customerId, topLevelCristinOrgId,
                                                              MANAGE_DOI, MANAGE_RESOURCES_STANDARD);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Publication.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = resourceService.getPublicationByIdentifier(savedPublication.getIdentifier());
        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        var expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        var actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    private static Set<CuratingInstitution> mockCuratingInstitutions(ArrayList<Contributor> contributors) {
        return contributors
                   .stream()
                   .map(UpdatePublicationHandlerTest::getAffiliationUriStream)
                   .flatMap(Set::stream)
                   .map(id -> new CuratingInstitution(id, Set.of(randomUri())))
                   .collect(Collectors.toSet());
    }

    private static Set<URI> getAffiliationUriStream(Contributor c) {
        return c.getAffiliations()
                   .stream()
                   .filter(a -> a instanceof Organization)
                   .map(b -> (Organization) b)
                   .map(Organization::getId)
                   .collect(Collectors.toSet());
    }

    @Test
    void shouldUpdateNonDegreePublicationWhenUserHasAccessRightEditAllNonDegreePublications()
        throws ApiGatewayException, IOException {
        var savedPublication = persistPublication(createNonDegreePublication().copy()).build();
        var publicationUpdate = updateTitle(savedPublication);
        var event = userWithEditAllNonDegreePublicationsUpdatesPublication(customerId, publicationUpdate);

        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Publication.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = resourceService.getPublicationByIdentifier(savedPublication.getIdentifier());

        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        var expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        var actualTitle = updatedPublication.getEntityDescription().getMainTitle();

        assertThat(actualTitle, is(equalTo(expectedTitle)));
        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @ParameterizedTest(name = "Should update degree publication when user has access rights to edit degree")
    @MethodSource("allProtectedDegreeInstances")
    void shouldUpdateDegreePublicationWhenUserHasAccessRightToEditDegree(Class<?> degree)
        throws BadRequestException, IOException, NotFoundException {
        var degreePublication = savePublication(randomPublicationWithPublisher(customerId, degree));
        var publicationUpdate = updateTitle(degreePublication);
        var event = userWithAccessRightToEditDegree(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var updatedPublication = resourceService.getPublicationByIdentifier(degreePublication.getIdentifier());

        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        var expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        var actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @ParameterizedTest(name = "Should update degree publication when user is resource owner")
    @MethodSource("allProtectedDegreeInstances")
    void shouldUpdateDegreePublicationWhenUserIsResourceOwner(Class<?> degree)
        throws BadRequestException, IOException, NotFoundException {
        var degreePublication = savePublication(randomPublicationWithPublisher(customerId, degree));
        var publicationUpdate = updateTitle(degreePublication);
        var event = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = resourceService.getPublicationByIdentifier(degreePublication.getIdentifier());
        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        var expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        var actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @ParameterizedTest(name = "Should return Unauthorized publication when user does not has access rights to edit "
                              + "degree and is not publication owner and the publication is Degree")
    @MethodSource("allProtectedDegreeInstances")
    void shouldReturnForbiddenWhenUserDoesNotHasAccessRightToEditDegree(Class<?> degree)
        throws BadRequestException, IOException {
        var degreePublication = savePublication(randomPublicationWithPublisher(customerId, degree));
        var publicationUpdate = updateTitle(degreePublication);
        var event = userWithEditAllNonDegreePublicationsUpdatesPublication(customerId, publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void handlerUpdatesDegreePublicationWhenInputIsValidAndUserIsExternalClient()
        throws IOException, BadRequestException {
        var thesisPublication = publication.copy().withEntityDescription(thesisPublishableEntityDescription()).build();
        var savedThesis = Resource
                              .fromPublication(thesisPublication)
                              .persistNew(resourceService, UserInstance.fromPublication(publication));
        var publicationUpdate = updateTitle(savedThesis);

        when(getExternalClientResponse.getCustomerUri())
            .thenReturn(publication.getPublisher().getId());
        when(getExternalClientResponse.getActingUser())
            .thenReturn(publication.getResourceOwner().getOwner().getValue());

        var inputStream = externalClientUpdatesPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        final var body = gatewayResponse.getBodyObject(PublicationResponseElevatedUser.class);
        assertThat(body.getEntityDescription().getMainTitle(),
                   is(equalTo(publicationUpdate.getEntityDescription().getMainTitle())));
    }

    @Test
    void shouldCompleteExistingPendingPublishingRequestWhenPublicationUpdateRemovesFilesAndFilesAreNotPublished()
        throws IOException, ApiGatewayException {
        var persistedPublication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(customerId,
                                                                                                 PUBLISHED,
                                                                                                 resourceService);
        var existingTicket = TicketEntry.requestNewTicket(persistedPublication, PublishingRequestCase.class)
                                 .withOwner(publication.getResourceOwner().getOwner().getValue())
                                 .withOwnerAffiliation(persistedPublication.getResourceOwner().getOwnerAffiliation())
                                 .persistNewTicket(ticketService);
        var updatedPublication = persistedPublication.copy().withAssociatedArtifacts(List.of()).build();
        var input = ownerUpdatesOwnPublication(updatedPublication.getIdentifier(), updatedPublication);
        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(existingTicket.fetch(ticketService).getStatus(), is(equalTo(TicketStatus.COMPLETED)));
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HTTP_OK)));
    }

    @Test
    void shouldSetFinalizedByForPublishingRequestToUserWhoUpdatesPublication()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistNonDegreePublicationWithoutFiles();
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        var publicationWithoutFiles = publication.copy().withAssociatedArtifacts(List.of()).build();
        injectContributor(publicationWithoutFiles, contributor);
        var resource = Resource.fromPublication(publication);
        resource.publish(resourceService, UserInstance.fromPublication(publication));
        var publicationUpdate = publicationWithoutFiles.copy().withDoi(randomUri()).build();

        var username = contributor.getIdentity().getName();
        var event = contributorUpdatesPublicationAndHasRightsToUpdate(publicationUpdate, cristinId,
                                                                      username);
        var pendingTicket = PublishingRequestCase.create(resource, UserInstance.create(username, randomUri()), PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
                                .persistNewTicket(ticketService);
        updatePublicationHandler.handleRequest(event, output, context);

        var completedTicket = ticketService.fetchTicket(pendingTicket);

        assertThat(completedTicket.getFinalizedBy(), is(equalTo(new Username(username))));
        assertThat(completedTicket.getFinalizedBy(), is(not(equalTo(publication.getResourceOwner().getOwner()))));
    }

    @Test
    void publishingCuratorWithAccessRightManageResourceFilesShouldBeAbleToOverrideRrs()
        throws IOException, NotFoundException, BadRequestException {
        var openFileRrs = File.builder()
                              .withIdentifier(randomUUID())
                              .withName(randomString())
                              .withSize(10L)
                              .withMimeType("application/pdf")
                              .withPublisherVersion(PublisherVersion.ACCEPTED_VERSION)
                              .withLicense(
                                  UriWrapper.fromUri("https://creativecommons.org/licenses/by/4.0").getUri())
                              .withRightsRetentionStrategy(CustomerRightsRetentionStrategy.create(
                                  RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY))
                              .buildOpenFile();
        var randomPublication = randomPublication(AcademicArticle.class).copy()
                                    .withStatus(PUBLISHED)
                                    .withAssociatedArtifacts(List.of(openFileRrs))
                                    .withPublisher(new Organization.Builder()
                                                       .withId(customerId)
                                                       .build()).build();

        var owner = getOwner(randomPublication);

        var contributorTopLevelCristinId = owner.getTopLevelOrgCristinId();

        var publicationWithRrs = Resource.fromPublication(randomPublication).copy()
                                     .withCuratingInstitutions(Set.of(
                                         new CuratingInstitution(contributorTopLevelCristinId, Set.of())))
                                     .build().persistNew(resourceService, owner);

        var resourceWithRrs = resourceService.getResourceByIdentifier(publicationWithRrs.getIdentifier());

        resourceWithRrs.getFileEntries()
            .forEach(file -> {
                file.getFile().setRightsRetentionStrategy(OverriddenRightsRetentionStrategy.create(
                    OVERRIDABLE_RIGHTS_RETENTION_STRATEGY, null));
            });

        var request = curatorWithAccessRightsUpdatesPublication(resourceWithRrs.toPublication(), customerId,
                                                                contributorTopLevelCristinId,
                                                                MANAGE_RESOURCE_FILES, MANAGE_RESOURCES_STANDARD);
        updatePublicationHandler.handleRequest(request, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());

        var updatedPublication = resourceService.getPublicationByIdentifier(resourceWithRrs.getIdentifier());
        var actualPublishedFile = (File) updatedPublication.getAssociatedArtifacts().getFirst();
        assertThat(actualPublishedFile.getRightsRetentionStrategy(),
                   allOf(instanceOf(OverriddenRightsRetentionStrategy.class),
                         hasProperty("overriddenBy", is(notNullValue()))));
    }

    private UserInstance getOwner(Publication randomPublication) {
        return new UserInstance(randomPublication.getResourceOwner().getOwner().getValue(),
                                customerId,
                                randomPublication.getResourceOwner().getOwnerAffiliation(),
                                null, null, List.of(), UserClientType.INTERNAL);
    }

    @Test
    void shouldUpdatePublicationWithoutReferencedContext()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithInternalFile(customerId,
                                                                                     resourceService);
        publication.getEntityDescription().getReference().setDoi(null);
        resourceService.updateResource(Resource.fromPublication(publication), UserInstance.fromPublication(publication));
        TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService)
            .complete(publication, randomUserInstance()).persistUpdate(ticketService);

        var newPendingOpenFile = File.builder().withIdentifier(randomUUID())
                                     .withLicense(randomUri()).buildPendingOpenFile();
        var files = new ArrayList<>(publication.getAssociatedArtifacts());
        files.add(newPendingOpenFile);

        publication.copy().withAssociatedArtifacts(files);

        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);
        updatePublicationHandler.handleRequest(input, output, context);

        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(), is(equalTo(200)));
    }

    private UserInstance randomUserInstance() {
        return UserInstance.create(randomString(), randomUri());
    }

    @Test
    void shouldReturnSuccessWhenUpdatingMetadataOnlyWhenPublishingFilesIsNotAllowedInCustomerConfiguration()
        throws BadRequestException, IOException {

        WireMock.reset();

        stubSuccessfulTokenResponse();
        stubSuccessfulCustomerResponseAllowingFilesForNoTypes(customerId);

        var savedPublication = createSamplePublication();
        var publicationUpdate = updateTitle(savedPublication);
        var event = userUpdatesPublicationAndHasRightToUpdate(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingAccessRightsWhenUnpublishingPublication()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistPublicationWithoutDoi(true);

        var unpublishRequest = new UnpublishPublicationRequest();
        unpublishRequest.setComment(COMMENT_ON_UNPUBLISHING_REQUEST);

        var inputStream = new HandlerRequestBuilder<UnpublishPublicationRequest>(restApiMapper)
                              .withUserName(randomString())
                              .withCurrentCustomer(RandomPersonServiceResponse.randomUri())
                              .withAccessRights(null)
                              .withBody(unpublishRequest)
                              .withPathParameters(
                                  Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                              .build();

        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotPublicationOwnerWhenUnpublishingPublication() throws IOException,
                                                                                                   ApiGatewayException {
        var publication = createAndPersistPublicationWithoutDoi(true);
        var inputStream = createUnpublishHandlerRequest(publication, randomString(),
                                                        RandomPersonServiceResponse.randomUri(),
                                                        randomUri());
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotContributorWhenUnpublishingPublication()
        throws IOException, ApiGatewayException {

        var userId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();
        var publication = createPublicationWithoutDoiAndWithContributor(userId, userName);
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var inputStream = createUnpublishHandlerRequest(publication, randomString(),
                                                        RandomPersonServiceResponse.randomUri(),
                                                        RandomPersonServiceResponse.randomUri());
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsContributorWhenUnpublishingPublicationAndPublicationContainsOpenFiles()
        throws ApiGatewayException, IOException {

        var userCristinId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();
        var publication = createPublicationWithoutDoiAndWithContributor(userCristinId, userName);

        assertTrue(publication.getAssociatedArtifacts().stream().anyMatch(OpenFile.class::isInstance));

        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var inputStream = createUnpublishHandlerRequest(publication, userName,
                                                        RandomPersonServiceResponse.randomUri(), userCristinId);
        updatePublicationHandler.handleRequest(inputStream, output, context);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(updatedPublication.getStatus(), is(equalTo(PUBLISHED)));
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsPublicationOwnerWhenUnpublishingPublicationAndPublicationContainsPublishedFiles()
        throws ApiGatewayException, IOException {
        var userCristinId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();
        var doi = RandomPersonServiceResponse.randomUri();
        var publication = createPublicationWithOwnerAndDoi(userCristinId, userName, doi);
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        var inputStream = createUnpublishHandlerRequest(publication, userName,
                                                        RandomPersonServiceResponse.randomUri(), userCristinId);
        updatePublicationHandler.handleRequest(inputStream, output, context);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(updatedPublication.getStatus(), is(equalTo(PUBLISHED)));
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldSetAllPendingAndNewTicketsToNotRelevantExceptUnpublishingTicketWhenUnpublishingPublicationWithoutPublishedFiles()
        throws ApiGatewayException, IOException {
        var userCristinId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();
        var publication = TicketTestUtils.createPersistedPublishedPublicationWithUnpublishedFilesAndContributor(
            userCristinId,
            resourceService);
        var resource = Resource.fromPublication(publication);
        var userInstance = UserInstance.fromPublication(publication);
        GeneralSupportRequest.create(resource, userInstance).persistNewTicket(ticketService);
        DoiRequest.create(resource, userInstance).persistNewTicket(ticketService);
        var publishingRequestTicket =
            PublishingRequestCase.create(resource, userInstance, PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
                .persistNewTicket(ticketService);
        var completedPublishingRequest = publishingRequestTicket.complete(publication, UserInstance.create(userName,
                                                                                                           randomUri()));
        ticketService.updateTicket(completedPublishingRequest);

        var inputStream = createUnpublishHandlerRequest(publication, userName,
                                                        RandomPersonServiceResponse.randomUri(), userCristinId);
        updatePublicationHandler.handleRequest(inputStream, output, context);
        var ticketsAfterUnpublishing =
            resourceService.fetchAllTicketsForResource(resource).toList();
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(UNPUBLISHED)));
        assertThat(ticketsAfterUnpublishing, hasItem(allOf(instanceOf(PublishingRequestCase.class),
                                                           hasProperty("status", equalTo(COMPLETED)))));
        assertThat(ticketsAfterUnpublishing, hasItem(allOf(instanceOf(DoiRequest.class),
                                                           hasProperty("status", equalTo(NOT_APPLICABLE)))));
        assertThat(ticketsAfterUnpublishing, hasItem(allOf(instanceOf(GeneralSupportRequest.class),
                                                           hasProperty("status", equalTo(NOT_APPLICABLE)))));
    }

    @ParameterizedTest()
    @DisplayName("User with access right should be able to unpublish publication with published files")
    @MethodSource("privilegedUserProvider")
    void shouldSetAllPendingAndNewTicketsToNotRelevantExceptUnpublishingTicketWhenCuratorUnpublishesPublicationWithPublishedFiles(
        AccessRight... accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithOpenFiles(customerId, PUBLISHED,
                                                                                  resourceService);
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication);
        GeneralSupportRequest.create(resource, userInstance).persistNewTicket(ticketService);
        DoiRequest.create(resource, userInstance).persistNewTicket(ticketService);
        PublishingRequestCase.create(resource, userInstance, PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
            .complete(publication, userInstance)
            .persistNewTicket(ticketService);
        var input = createUnpublishHandlerRequest(publication, randomString(), customerId, accessRight);
        updatePublicationHandler.handleRequest(input, output, context);

        var unpublishedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticketsAfterUnpublishing =
            resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication)).toList();

        assertThat(unpublishedPublication.getStatus(), is(equalTo(UNPUBLISHED)));
        assertThat(ticketsAfterUnpublishing, hasItem(allOf(instanceOf(DoiRequest.class),
                                                           hasProperty("status", equalTo(NOT_APPLICABLE)))));
        assertThat(ticketsAfterUnpublishing, hasItem(allOf(instanceOf(GeneralSupportRequest.class),
                                                           hasProperty("status", equalTo(NOT_APPLICABLE)))));
        assertThat(ticketsAfterUnpublishing, hasItem(allOf(instanceOf(PublishingRequestCase.class),
                                                           hasProperty("status", equalTo(COMPLETED)))));
    }

    @Test
    void shouldProduceUpdateDoiEventWhenUnpublishingIsSuccessful()
        throws ApiGatewayException, IOException {
        var userCristinId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();
        var publication =
            TicketTestUtils.createPersistedPublishedPublicationWithUnpublishedFilesAndContributor(userCristinId,
                                                                                                  resourceService);

        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var inputStream = createUnpublishHandlerRequest(publication, userName,
                                                        RandomPersonServiceResponse.randomUri(), userCristinId);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        assertTrue(eventBridgeClient.getRequestEntries()
                       .stream()
                       .anyMatch(entry -> entry.source().equals(NVA_PUBLICATION_DELETE_SOURCE)
                                          && entry.detailType().equals(LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS)));
    }

    @Test
    void shouldThrowExceptionOnFailingDuplicateOfValidationWhenUnpublishing()
        throws Exception {
        var userCristinId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();
        var doi = RandomPersonServiceResponse.randomUri();
        var duplicate = URI.create("https://badactor.org/publication/" + SortableIdentifier.next());
        var publication = createPublicationWithOwnerAndDoi(userCristinId, userName, doi);

        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var inputStream = createUnpublishRequestWithDuplicateOfValue(publication, userName,
                                                                     RandomPersonServiceResponse.randomUri(),
                                                                     duplicate);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var gatewayResponse = toGatewayResponseProblem();

        assertThat(gatewayResponse.getStatusCode(), Is.is(IsEqual.equalTo(SC_BAD_REQUEST)));
        assertThat(getProblemDetail(gatewayResponse), containsString(MUST_BE_A_VALID_PUBLICATION_API_URI));
    }

    @Test
    void shouldProduceUpdateDoiEventWithDuplicateWhenUnpublishing()
        throws ApiGatewayException, IOException {
        var userName = randomString();
        var duplicate = randomPublicationApiUri();
        var publication =
            TicketTestUtils.createPersistedPublishedPublicationWithUnpublishedFilesAndOwner(userName, resourceService);

        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var inputStream = createUnpublishRequestWithDuplicateOfValue(publication, userName,
                                                                     RandomPersonServiceResponse.randomUri(),
                                                                     duplicate);

        updatePublicationHandler.handleRequest(inputStream, output, context);

        assertThat(eventBridgeClient.getRequestEntries()
                       .stream()
                       .filter(a -> a.source().equals(NVA_PUBLICATION_DELETE_SOURCE))
                       .map(UpdatePublicationHandlerTest::getDoiMetadataUpdateEvent)
                       .map(LambdaDestinationInvocationDetail::responsePayload)
                       .filter(a -> nonNull(a.getDuplicateOf()))
                       .findFirst()
                       .orElseThrow()
                       .getDuplicateOf(),
                   Is.is(IsEqual.equalTo(duplicate)));
    }

    private static URI randomPublicationApiUri() {
        return URI.create("https://" + API_HOST_DOMAIN + "/" + PUBLICATION + "/" + SortableIdentifier.next());
    }

    @Test
    void shouldReturnSuccessWhenUserIsResourceOwnerWhenUnpublishingPublication()
        throws ApiGatewayException, IOException {

        var userName = randomString();
        var institutionId = RandomPersonServiceResponse.randomUri();
        var publication =
            TicketTestUtils.createPersistedPublishedPublicationWithUnpublishedFilesAndOwner(userName, resourceService);
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var inputStream = createUnpublishHandlerRequest(publication, userName, institutionId);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUnpublishingNotPublishedPublication() throws IOException, ApiGatewayException {
        var unpublishedPublication = createAndPersistPublicationWithoutDoi(false);
        var publisherUri = unpublishedPublication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(unpublishedPublication, randomString(),
                                                        publisherUri,
                                                        AccessRight.MANAGE_RESOURCES_ALL);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnBadRequestWhenCommentIsMissingWhenUnpublishingPublication()
        throws ApiGatewayException, IOException {
        var userName = randomString();
        var institutionId = RandomPersonServiceResponse.randomUri();
        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(userName, institutionId);
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var unpublishRequest = new UnpublishPublicationRequest();
        var request = new HandlerRequestBuilder<UnpublishPublicationRequest>(restApiMapper)
                          .withUserName(userName)
                          .withCurrentCustomer(institutionId)
                          .withBody(unpublishRequest)
                          .withPersonCristinId(randomUri())
                          .withTopLevelCristinOrgId(randomUri())
                          .withPathParameters(
                              Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()));

        updatePublicationHandler.handleRequest(request.build(), output, context);
        var gatewayResponse = toGatewayResponseProblem();

        assertThat(gatewayResponse.getStatusCode(), Is.is(IsEqual.equalTo(SC_BAD_REQUEST)));
        assertThat(getProblemDetail(gatewayResponse), containsString(UNPUBLISH_REQUEST_REQUIRES_A_COMMENT));
    }

    @Test
    void shouldReturnNotFoundWhenPublicationDoesNotExist() throws IOException {
        var inputStream = createUnpublishHandlerRequest(randomPublication(), randomString(),
                                                        RandomPersonServiceResponse.randomUri());
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_NOT_FOUND)));
    }

    @Test
    void shouldReturnUnauthorizedWhenDeletingUnsupportedPublicationStatus() throws IOException {
        Publication persistedPublication = persistPublication(createNonDegreePublication()
                                                                  .copy()
                                                                  .withStatus(PublicationStatus.NEW))
                                               .build();

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(persistedPublication, randomString(),
                                                        publisherUri,
                                                        AccessRight.MANAGE_RESOURCES_ALL);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnSuccessAndUpdatePublicationStatusToUnpublishedWhenUserCanEditOwnInstitutionResources()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistPublicationWithoutDoi(true);
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(publication, randomString(), publisherUri,
                                                        AccessRight.MANAGE_RESOURCES_STANDARD,
                                                        MANAGE_PUBLISHING_REQUESTS);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));

        var unpublishedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(unpublishedPublication.getStatus(), Is.is(IsEqual.equalTo(PublicationStatus.UNPUBLISHED)));
    }

    @Test
    void shouldReturnSuccessWhenEditorUnpublishesDegree() throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(publication, randomString(), publisherUri,
                                                        MANAGE_DEGREE, MANAGE_RESOURCES_ALL);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenNonEditorUnpublishesDegree() throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(publication, randomString(), publisherUri);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldUpdateUnpublishedResourceWithDuplicateOfValueWhenUserIsCurator()
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        var duplicate = randomPublicationApiUri();
        var request = createUnpublishRequestWithDuplicateOfValue(publication,
                                                                 randomString(),
                                                                 publication.getPublisher().getId(),
                                                                 duplicate,
                                                                 MANAGE_DEGREE, MANAGE_RESOURCES_STANDARD,
                                                                 MANAGE_PUBLISHING_REQUESTS);
        updatePublicationHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
        assertThat(updatedPublication.getDuplicateOf(), Is.is(IsEqual.equalTo(duplicate)));
    }

    @Test
    void shouldReturnSuccessWhenCuratorUnpublishesPublishedPublicationFromOwnInstitution()
        throws ApiGatewayException, IOException {

        var institutionId = RandomPersonServiceResponse.randomUri();
        var curatorUsername = randomString();
        var resourceOwnerUsername = randomString();

        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(resourceOwnerUsername,
                                                                                    institutionId);
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var inputStream = createUnpublishHandlerRequest(publication, curatorUsername, institutionId,
                                                        MANAGE_RESOURCES_STANDARD, MANAGE_PUBLISHING_REQUESTS);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenCuratorUnpublishesPublishedPublicationFromAnotherInstitution()
        throws IOException, ApiGatewayException {
        var curatorUsername = randomString();
        var curatorInstitutionId = RandomPersonServiceResponse.randomUri();
        var resourceOwnerUsername = randomString();
        var resourceOwnerInstitutionId = RandomPersonServiceResponse.randomUri();
        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(resourceOwnerUsername,
                                                                                    resourceOwnerInstitutionId);
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));

        var inputStream = createUnpublishHandlerRequestForTopLevelCristinOrg(publication, curatorUsername,
                                                                             curatorInstitutionId, randomUri(),
                                                                             AccessRight.MANAGE_RESOURCES_STANDARD);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldDeleteUnpublishedPublicationWhenUserIsEditor()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublication();
        var customer = publication.getPublisher().getId();

        var request = creatDeleteHandlerRequest(publication.getIdentifier(),
                                                publication.getResourceOwner().getOwner().getValue(), customer,
                                                publication.getResourceOwner().getOwnerAffiliation(), null,
                                                MANAGE_RESOURCES_ALL,
                                                MANAGE_DEGREE);
        updatePublicationHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var deletePublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
        assertThat(deletePublication.getStatus(), Is.is(IsEqual.equalTo(PublicationStatus.DELETED)));
    }

    @Test
    void ownerUserShouldGetUnauthorizedWhenHardDeletePublication()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublication();
        var publisherUri = publication.getPublisher().getId();
        var request = creatDeleteHandlerRequest(publication.getIdentifier(),
                                                publication.getResourceOwner().getOwner().getValue(),
                                                publisherUri, publication.getResourceOwner().getOwnerAffiliation(),
                                                null);
        updatePublicationHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void handlerUpdatesPublicationShouldReturnAllowedOperations()
        throws IOException, ApiGatewayException {
        publication = publicationWithoutIdentifier(customerId);
        var savedPublication = createSamplePublication();
        var publicationUpdate = updateTitle(savedPublication);
        var event = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponseElevatedUser.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        final var body = gatewayResponse.getBodyObject(PublicationResponseElevatedUser.class);
        assertThat(body.getAllowedOperations(), hasItems(UPDATE, DELETE));
    }

    @Test
    void shouldThrowExceptionWhenNonCuratorAttemptsToRemovePublishedFile()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithOpenFiles(customerId, PUBLISHED,
                                                                                  resourceService);
        var updatedPublication = publication.copy().withAssociatedArtifacts(Collections.emptyList()).build();
        var event = ownerUpdatesOwnPublication(updatedPublication.getIdentifier(), updatedPublication);

        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_UNAUTHORIZED)));
    }

    @ParameterizedTest
    @EnumSource(value = AccessRight.class, mode = Mode.EXCLUDE, names = {"MANAGE_RESOURCE_FILES"})
    void shouldNotAllowUserWithoutAccessRightManageResourceFilesToRemovePublishedFile(AccessRight accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithOpenFiles(customerId, PUBLISHED,
                                                                                  resourceService);
        var updatedPublication = publication.copy().withAssociatedArtifacts(Collections.emptyList()).build();
        var event = curatorWithAccessRightsUpdatesPublication(updatedPublication, customerId,
                                                              publication.getResourceOwner().getOwnerAffiliation(),
                                                              accessRight);

        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_UNAUTHORIZED)));
    }

    @Test
    void publicationOwnerShouldNotBeAbleToUpdateMetadataOfOpenFile() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithOpenFiles(customerId, PUBLISHED,
                                                                                  resourceService);
        var openFile = publication.getAssociatedArtifacts().stream()
                           .filter(OpenFile.class::isInstance)
                           .map(OpenFile.class::cast)
                           .findFirst().orElseThrow();
        var updatedFile = openFile.copy().withLicense(randomUri()).buildOpenFile();
        var files = publication.getAssociatedArtifacts();
        files.remove(openFile);
        files.add(updatedFile);
        var event = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);

        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_UNAUTHORIZED)));
    }

    @Test
    void userWhichHasAccessRightManageResourcesAllAndIsPartOfPublicationCuratingInstitutionsRepublishPublication()
        throws ApiGatewayException,
               IOException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of(randomUri()))));
        var userInstance = UserInstance.fromPublication(publication);
        resourceService.unpublishPublication(publication, userInstance);
        var input = curatorWithAccessRightsRepublishedPublication(publication, randomUri(), curatingInstitution,
                                                                  MANAGE_RESOURCES_ALL);

        updatePublicationHandler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        var republishedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(republishedPublication.getStatus(), is(equalTo(PUBLISHED)));
    }

    @Test
    void shouldReturnForbiddenWhenRepublishingUnpublishedPublication()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of(randomUri()))));
        var input = curatorWithAccessRightsRepublishedPublication(publication, randomUri(), curatingInstitution,
                                                                  MANAGE_RESOURCES_ALL);

        updatePublicationHandler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnForbiddenWhenUserRepublishesPublicationAndUserInstitutionIsNotInCuratingInstitutions()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var input = curatorWithAccessRightsRepublishedPublication(publication, randomUri(), randomUri(),
                                                                  MANAGE_RESOURCES_ALL);

        updatePublicationHandler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @DisplayName("When user uploads file and customer does not allow publishing files, then PublishingRequestCase is" +
                 "persisted containing file to publish in filesForApproval")
    @Test
    void shouldSetFilesForApprovalWhenPublicationOwnerUpdatesPublicationWithUnpublishedFile()
        throws ApiGatewayException,
               IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithOpenFiles(
            customerId, PUBLISHED, resourceService);

        var pendingOpenFile = File.builder().withIdentifier(randomUUID())
                                  .withLicense(randomUri())
                                  .buildPendingOpenFile();
        updatePublicationWithFile(publication, pendingOpenFile);

        stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(customerId);
        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);
        updatePublicationHandler.handleRequest(input, output, context);

        var publishingRequest = getPublishingRequestCase(publication);

        assertThat(publishingRequest.getFilesForApproval().stream().map(File::getIdentifier).toList(),
                   containsInAnyOrder(pendingOpenFile.getIdentifier()));
    }

    @DisplayName("When contributor uploads file and customer does not allow publishing files, then " +
                 "pending PublishingRequestCase is persisted containing file to publish in filesForApproval")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#notApprovedFilesProvider")
    void shouldSetFilesForApprovalWhenContributorUpdatesPublicationWithUnpublishedFile(File newUnpublishedFile)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithOpenFiles(
            customerId, PUBLISHED, resourceService);
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        injectContributor(publication, contributor);
        updatePublicationWithFile(publication, newUnpublishedFile);

        stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(customerId);
        var input = contributorsUpdatesPublication(publication, cristinId, randomUri());
        updatePublicationHandler.handleRequest(input, output, context);

        var publishingRequest = getPublishingRequestCase(publication);

        assertThat(publishingRequest.getStatus(), is(equalTo(PENDING)));
        assertThat(publishingRequest.getFilesForApproval().stream().map(File::getIdentifier).toList(),
                   containsInAnyOrder(newUnpublishedFile.getIdentifier()));
    }

    @DisplayName("When user with accessRight updates metadata for publication containing unpublished file and" +
                 " there exists pending PublishingRequest containing unpublished file in files for approval," +
                 " and customer does not allow publishing files " +
                 " then unpublished file is not being published and still exists in files for approval")
    @Test
    void shouldNotPublishFilesWhenUpdatingMetadataForPublicationWithUnpublishedFiles()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(
            customerId, PUBLISHED, resourceService);
        persistPublishingRequestContainingExistingUnpublishedFiles(publication);
        var updatedPublication = updateTitle(publication);

        stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(customerId);
        var input = curatorWithAccessRightsUpdatesPublication(updatedPublication, customerId,
                                                              publication.getResourceOwner().getOwnerAffiliation(),
                                                              SUPPORT, MANAGE_OWN_RESOURCES, MANAGE_NVI_CANDIDATES,
                                                              MANAGE_RESOURCES_STANDARD);
        updatePublicationHandler.handleRequest(input, output, context);

        var publishingRequest = getPublishingRequestCase(publication);
        var unpublishedFile = getUnpublishedFiles(publication).getFirst();
        assertThat(publishingRequest.getFilesForApproval(), hasItem(unpublishedFile));
    }

    @DisplayName("When contributor updates publication with unpublished file" +
                 " and publication already contains another unpublished file" +
                 " and there is no pending publishing request for the publication with contributor owner affiliation" +
                 " then only new unpublished file is added to files for approval in PublishingRequest")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#notApprovedFilesProvider")
    void shouldAddOnlyNewUnpublishedFilesForApprovalToPublishingRequestsFilesForApprovalWhenAddingNewUnpublishedFile(
        File newUnpublishedFile)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(
            customerId, PUBLISHED, resourceService);
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        injectContributor(publication, contributor);
        updatePublicationWithFile(publication, newUnpublishedFile);
        stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(customerId);
        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);
        updatePublicationHandler.handleRequest(input, output, context);

        var publishingRequest = getPublishingRequestCase(publication);

        assertThat(publishingRequest.getFilesForApproval().stream().map(File::getIdentifier).toList(),
                   containsInAnyOrder(newUnpublishedFile.getIdentifier()));
    }

    @Test
    void shouldUpdateFileMetadataOnUpdatePublicationRequest()
        throws IOException, BadRequestException {
        var file = randomPendingOpenFile();
        var publication = randomNonDegreePublication().copy()
                              .withPublisher(Organization.fromUri(customerId))
                              .withAssociatedArtifacts(List.of())
                              .build();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        FileEntry.create(file, resource.getIdentifier(),
                         UserInstance.fromPublication(publication))
            .persist(resourceService);
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        injectContributor(resource, contributor);
        var updatedFile = file.copy().buildPendingInternalFile();
        var updatedPublication = resource.copy().withAssociatedArtifacts(List.of(updatedFile)).build();
        stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(customerId);
        var input = ownerUpdatesOwnPublication(resource.getIdentifier(), updatedPublication);

        var resourceService = getResourceServiceBuilder().build();
        var handler = new UpdatePublicationHandler(resourceService, ticketService, environment, identityServiceClient,
                                                   eventBridgeClient, secretsManagerClient,
                                                   WiremockHttpClient.create());
        handler.handleRequest(input, output, context);

        assertEquals(updatedFile,
                     FileEntry.queryObject(file.getIdentifier(), resource.getIdentifier())
                         .fetch(resourceService).orElseThrow().getFile());
    }

    @Test
    void shouldReturnNotFoundWhenFetchingPublicationWithFilesAndPublicationDoesNotExist() throws IOException {
        var publication = randomPublication();
        stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(customerId);
        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);

        var handler = new UpdatePublicationHandler(resourceService, ticketService, environment, identityServiceClient,
                                                   eventBridgeClient, secretsManagerClient,
                                                   WiremockHttpClient.create());
        handler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    @Test
    void shouldThrowForbiddenWhenUpdatingFileToOpenPendingFileWhenCustomerDoesNotAllowOpenPendingFilesForPublication()
        throws IOException, ApiGatewayException {
        var uploadedFile = randomUploadedFile();
        var publication = randomPublication(JournalArticle.class).copy()
                              .withStatus(DRAFT)
                              .withAssociatedArtifacts(List.of(uploadedFile))
                              .withPublisher(Organization.fromUri(customerId))
                              .build();
        publication = Resource.fromPublication(publication).persistNew(resourceService, UserInstance.fromPublication(publication));
        stubSuccessfulCustomerResponseAllowingFilesForNoTypes(customerId);
        var file = ((File) publication.getAssociatedArtifacts().getFirst()).toPendingOpenFile();
        var publicationUpdate = publication.copy()
                                    .withAssociatedArtifacts(List.of(file))
                                    .build();
        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publicationUpdate);

        var handler = new UpdatePublicationHandler(resourceService, ticketService, environment, identityServiceClient,
                                                   eventBridgeClient, secretsManagerClient,
                                                   WiremockHttpClient.create());
        handler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void curatorShouldbeAbleToUpdateFileToOpenPendingFileWhenCustomerDoesNotAllowOpenPendingFilesForPublication()
        throws IOException, ApiGatewayException {
        var uploadedFile = randomUploadedFile();
        var publication = randomPublication(JournalArticle.class).copy()
                              .withStatus(DRAFT)
                              .withAssociatedArtifacts(List.of(uploadedFile))
                              .withPublisher(Organization.fromUri(customerId))
                              .build();
        publication = Resource.fromPublication(publication).persistNew(resourceService, UserInstance.fromPublication(publication));
        stubSuccessfulCustomerResponseAllowingFilesForNoTypes(customerId);
        var file = ((File) publication.getAssociatedArtifacts().getFirst()).toPendingOpenFile();
        var publicationUpdate = publication.copy()
                                    .withAssociatedArtifacts(List.of(file))
                                    .build();
        var input = curatorPublicationOwnerUpdatesPublication(publicationUpdate);

        var handler = new UpdatePublicationHandler(resourceService, ticketService, environment, identityServiceClient,
                                                   eventBridgeClient, secretsManagerClient,
                                                   WiremockHttpClient.create());
        handler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @Test
    void ownerShouldBeAbleToUpdateFileToPendingOpenWhenCustomerAllowsOpenPendingFilesAndPublicationIsMissingType()
        throws IOException, ApiGatewayException {
        var uploadedFile = randomUploadedFile();
        var publication = randomPublication(JournalArticle.class).copy()
                              .withStatus(DRAFT)
                              .withAssociatedArtifacts(List.of(uploadedFile))
                              .withEntityDescription(new EntityDescription())
                              .withPublisher(Organization.fromUri(customerId))
                              .build();
        publication = Resource.fromPublication(publication).persistNew(resourceService, UserInstance.fromPublication(publication));
        var file = ((File) publication.getAssociatedArtifacts().getFirst()).toPendingOpenFile();
        var publicationUpdate = publication.copy()
                                    .withAssociatedArtifacts(List.of(file))
                                    .withEntityDescription(randomEntityDescription(JournalArticle.class))
                                    .build();
        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publicationUpdate);

        var handler = new UpdatePublicationHandler(resourceService, ticketService, environment, identityServiceClient,
                                                   eventBridgeClient, secretsManagerClient,
                                                   WiremockHttpClient.create());
        handler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @Test
    void shouldNotBeAbleToUpdatePublicationWithPendingOpenFileToTypeWhichIsNotAllowedByCustomer()
        throws IOException, ApiGatewayException {
        var uploadedFile = randomPendingOpenFile();
        var publication = randomPublication(JournalArticle.class).copy()
                              .withStatus(DRAFT)
                              .withAssociatedArtifacts(List.of(uploadedFile))
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withPublisher(Organization.fromUri(customerId))
                              .build();
        publication = Resource.fromPublication(publication).persistNew(resourceService, UserInstance.fromPublication(publication));
        var file = ((File) publication.getAssociatedArtifacts().getFirst()).toPendingOpenFile();
        var publicationUpdate = publication.copy()
                                    .withAssociatedArtifacts(List.of(file))
                                    .withEntityDescription(randomEntityDescription(ConferenceAbstract.class))
                                    .build();
        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publicationUpdate);
        stubSuccessfulCustomerResponseAllowingFilesForNoTypes(customerId);
        var handler = new UpdatePublicationHandler(resourceService, ticketService, environment, identityServiceClient,
                                                   eventBridgeClient, secretsManagerClient,
                                                   WiremockHttpClient.create());
        handler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Publication.class);

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    private void persistPublishingRequestContainingExistingUnpublishedFiles(Publication publication)
        throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createNewTicket(publication,
                                                                                              PublishingRequestCase.class,
                                                                                              SortableIdentifier::next)
                                                            .withOwner(
                                                                publication.getResourceOwner().getOwner().getValue())
                                                            .withOwnerAffiliation(
                                                                publication.getResourceOwner().getOwnerAffiliation());
        publishingRequest.withFilesForApproval(TicketTestUtils.getFilesForApproval(publication));
        publishingRequest.persistNewTicket(ticketService);
    }

    private PublishingRequestCase getPublishingRequestCase(Publication publication) {
        return ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                             publication.getIdentifier(), PublishingRequestCase.class)
                   .orElseThrow();
    }

    private Publication createUnpublishedPublication() throws ApiGatewayException {
        var publication = createAndPersistDegreeWithoutDoi();
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        var userInstance = UserInstance.fromPublication(publication);
        resourceService.unpublishPublication(resourceService.getPublicationByIdentifier(publication.getIdentifier()), userInstance);
        return resourceService.getPublicationByIdentifier(publication.getIdentifier());
    }

    private InputStream createUnpublishHandlerRequest(Publication publication, String username,
                                                      URI institutionId, AccessRight... accessRight)
        throws JsonProcessingException {

        return createUnpublishHandlerRequest(publication, username, institutionId, null, accessRight);
    }

    private InputStream createUnpublishHandlerRequest(Publication publication, String username,
                                                      URI institutionId, URI cristinId, AccessRight... accessRight)
        throws JsonProcessingException {
        var unpublishRequest = new UnpublishPublicationRequest();
        unpublishRequest.setComment(COMMENT_ON_UNPUBLISHING_REQUEST);
        var request = new HandlerRequestBuilder<UnpublishPublicationRequest>(restApiMapper)
                          .withUserName(username)
                          .withCurrentCustomer(institutionId)
                          .withAccessRights(institutionId, accessRight)
                          .withBody(unpublishRequest)
                          .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                          .withPersonCristinId(randomUri())
                          .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()));

        if (nonNull(cristinId)) {
            request.withPersonCristinId(cristinId);
        }

        return request.build();
    }

    private InputStream createUnpublishHandlerRequestForTopLevelCristinOrg(Publication publication, String username,
                                                                           URI institutionId,
                                                                           URI topLevelCristinOrg,
                                                                           AccessRight... accessRight)
        throws JsonProcessingException {
        var unpublishRequest = new UnpublishPublicationRequest();
        unpublishRequest.setComment(COMMENT_ON_UNPUBLISHING_REQUEST);
        var request = new HandlerRequestBuilder<UnpublishPublicationRequest>(restApiMapper)
                          .withUserName(username)
                          .withCurrentCustomer(institutionId)
                          .withAccessRights(institutionId, accessRight)
                          .withBody(unpublishRequest)
                          .withTopLevelCristinOrgId(topLevelCristinOrg)
                          .withPersonCristinId(randomUri())
                          .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()));

        return request.build();
    }

    private InputStream creatDeleteHandlerRequest(SortableIdentifier publicationIdentifier, String username,
                                                  URI customer, URI topLevelCristinOrgId, URI cristinId,
                                                  AccessRight... accessRight)
        throws JsonProcessingException {
        var request = new HandlerRequestBuilder<DeletePublicationRequest>(restApiMapper)
                          .withUserName(username)
                          .withCurrentCustomer(customer)
                          .withTopLevelCristinOrgId(topLevelCristinOrgId)
                          .withAccessRights(customer, accessRight)
                          .withBody(new DeletePublicationRequest())
                          .withPersonCristinId(randomUri())
                          .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString()));

        if (nonNull(cristinId)) {
            request.withPersonCristinId(cristinId);
        }

        return request.build();
    }

    private InputStream createUnpublishRequestWithDuplicateOfValue(Publication publication, String username,
                                                                   URI institutionId,
                                                                   URI duplicateOf,
                                                                   AccessRight... accessRight)
        throws JsonProcessingException {
        var unpublishRequest = new UnpublishPublicationRequest();
        unpublishRequest.setComment(COMMENT_ON_UNPUBLISHING_REQUEST);
        unpublishRequest.setDuplicateOf(duplicateOf);
        var request = new HandlerRequestBuilder<UnpublishPublicationRequest>(restApiMapper)
                          .withUserName(username)
                          .withCurrentCustomer(institutionId)
                          .withAccessRights(institutionId, accessRight)
                          .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                          .withPersonCristinId(randomUri())
                          .withBody(unpublishRequest)
                          .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()));

        return request.build();
    }

    private Publication createAndPersistPublicationWithoutDoi(boolean shouldBePublished) throws ApiGatewayException {
        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withDoi(null).build();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));

        if (shouldBePublished) {
            Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        }

        return persistedPublication;
    }

    private Publication createPublicationWithContributorAndDoi(URI contributorId, String contributorName)
        throws ApiGatewayException {

        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .build();
        var identity = new Identity.Builder().withName(contributorName).withId(contributorId).build();
        var contributor = new Contributor.Builder().withIdentity(identity).withRole(new RoleType(Role.CREATOR)).build();
        var entityDesc = publication.getEntityDescription().copy().withContributors(List.of(contributor)).build();
        var publicationWithContributor = publication.copy().withEntityDescription(entityDesc).build();
        return Resource.fromPublication(publicationWithContributor)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication createPublicationWithOwnerAndDoi(URI contributorId, String owner,
                                                         URI doi)
        throws ApiGatewayException {
        var identity = new Identity.Builder().withName(owner).withId(contributorId).build();
        var contributor = new Contributor.Builder().withIdentity(identity).withRole(new RoleType(Role.CREATOR)).build();
        var entityDescription = randomEntityDescription(JournalArticle.class).copy()
                                    .withContributors(List.of(contributor))
                                    .build();
        var publication = randomPublication().copy()
                              .withEntityDescription(entityDescription)
                              .withDuplicateOf(null)
                              .withResourceOwner(
                                  new ResourceOwner(new Username(owner), RandomPersonServiceResponse.randomUri()))
                              .withDoi(doi).build();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication createPublicationWithoutDoiAndWithContributor(URI contributorId, String contributorName)
        throws ApiGatewayException {
        return createPublicationWithContributorAndDoi(contributorId, contributorName);
    }

    private Publication createAndPersistDegreeWithoutDoi() throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).build();
        var degreePhd = new DegreePhd(new MonographPages(), new PublicationDate(),
                                      Set.of(UnconfirmedDocument.fromValue(randomString())));
        var reference = new Reference.Builder().withPublicationInstance(degreePhd).build();
        var entityDescription = publication.getEntityDescription().copy().withReference(reference).build();
        var publicationOfTypeDegree = publication.copy().withEntityDescription(entityDescription).build();
        return Resource.fromPublication(publicationOfTypeDegree)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private Publication createAndPersistPublicationWithoutDoiAndWithResourceOwner(String userName, URI institution)
        throws BadRequestException {
        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withDoi(null)
                              .withResourceOwner(new ResourceOwner(new Username(userName), institution))
                              .withPublisher(new Organization.Builder().withId(institution).build())
                              .build();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private static LambdaDestinationInvocationDetail<DoiMetadataUpdateEvent> getDoiMetadataUpdateEvent(
        PutEventsRequestEntry a) {
        try {
            return JsonUtils.dtoObjectMapper.readValue(a.detail(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void updatePublicationWithFile(Publication publication, File newUnpublishedFile) {
        var associatedArtifacts = publication.getAssociatedArtifacts();
        associatedArtifacts.add(newUnpublishedFile);
        publication.setAssociatedArtifacts(associatedArtifacts);
    }

    private static List<File> getUnpublishedFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(PendingFile.class::isInstance)
                   .map(File.class::cast)
                   .toList();
    }

    private Publication savePublication(Publication publication) throws BadRequestException {
        var userInstance = UserInstance.fromPublication(publication);
        return Resource.fromPublication(publication).persistNew(resourceService, userInstance);
    }

    private List<Contributor> getRandomContributorsWithoutCristinIdAndIdentity() {
        var contributorWithoutCristinId = new Contributor.Builder()
                                              .withRole(new RoleType(Role.ARCHITECT))
                                              .withIdentity(new Identity.Builder().withName(randomString()).build())
                                              .build();
        var contributorWithoutIdentity = new Contributor.Builder()
                                             .withRole(new RoleType(Role.ARCHITECT))
                                             .build();
        return List.of(contributorWithoutCristinId, contributorWithoutIdentity);
    }

    private void injectContributor(Publication savedPublication, Contributor contributor) {
        var contributors = new ArrayList<>(savedPublication.getEntityDescription().getContributors());
        contributors.add(contributor);
        savedPublication.getEntityDescription().setContributors(contributors);
        resourceService.updateResource(Resource.fromPublication(savedPublication), UserInstance.fromPublication(publication));
    }

    private Contributor createContributorForPublicationUpdate(URI cristinId) {
        return new Contributor.Builder()
                   .withRole(new RoleType(Role.CREATOR))
                   .withIdentity(new Identity.Builder().withId(cristinId).withName(randomInteger().toString()).build())
                   .withAffiliations(getListOfRandomOrganizations())
                   .build();
    }

    private List<Corporation> getListOfRandomOrganizations() {
        return List.of(new Organization.Builder().withId(RandomPersonServiceResponse.randomUri()).build());
    }

    private boolean containsOneCompletedAndOnePendingPublishingRequest(List<TicketEntry> tickets) {
        var statuses = tickets.stream().map(TicketEntry::getStatus).toList();
        return statuses.stream().anyMatch(TicketStatus.COMPLETED::equals)
               && statuses.stream().anyMatch(TicketStatus.PENDING::equals);
    }

    private TicketEntry createPendingPublishingRequest(Publication publishedPublication) throws ApiGatewayException {
        return PublishingRequestCase.createNewTicket(publishedPublication, PublishingRequestCase.class,
                                                     SortableIdentifier::next)
                   .withOwner(publication.getResourceOwner().getOwner().getValue())
                   .withOwnerAffiliation(publishedPublication.getResourceOwner().getOwnerAffiliation())
                   .persistNewTicket(ticketService);
    }

    private void persistCompletedPublishingRequest(Publication publishedPublication) throws ApiGatewayException {
        var ticket = PublishingRequestCase.createNewTicket(publishedPublication, PublishingRequestCase.class,
                                                           SortableIdentifier::next)
                         .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation())
                         .withOwner(publication.getResourceOwner().getOwner().getValue())
                         .persistNewTicket(ticketService);
        ticketService.updateTicketStatus(ticket, TicketStatus.COMPLETED, UserInstance.create(randomString(), randomUri()));
    }

    private Publication createSamplePublication() throws BadRequestException {
        var userInstance = UserInstance.fromPublication(publication);
        return Resource.fromPublication(publication).persistNew(resourceService, userInstance);
    }

    private Publication createAndPersistNonDegreePublication() throws BadRequestException {
        var publication = randomNonDegreePublicationWithPublisher();
        var userInstance = UserInstance.fromPublication(publication);
        return Resource.fromPublication(publication).persistNew(resourceService, userInstance);
    }

    private Publication createAndPersistNonDegreePublicationWithoutFiles() throws BadRequestException {
        var publication = randomNonDegreePublicationWithPublisher();
        publication.setAssociatedArtifacts(AssociatedArtifactList.empty());
        var userInstance = UserInstance.fromPublication(publication);
        return Resource.fromPublication(publication).persistNew(resourceService, userInstance);
    }

    private InputStream requestWithoutUsername(Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(randomUri())
                   .withBody(publicationUpdate)
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream userWithAccessRightToEditDegree(Publication publicationUpdate) throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_DEGREE, MANAGE_RESOURCES_ALL)
                   .withTopLevelCristinOrgId(publicationUpdate.getResourceOwner().getOwnerAffiliation())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream userWithEditAllNonDegreePublicationsUpdatesPublication(URI customerId,
                                                                               Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_RESOURCES_ALL)
                   .withUserName(SOME_CURATOR)
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream userUpdatesPublicationOfOtherInstitution(Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = randomUri();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD)
                   .withUserName(SOME_CURATOR)
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream contributorUpdatesPublicationAndHasRightsToUpdate(Publication publicationUpdate, URI cristinId)
        throws JsonProcessingException {
        return contributorUpdatesPublicationAndHasRightsToUpdate(publicationUpdate, cristinId, SOME_CONTRIBUTOR);
    }

    private InputStream contributorUpdatesPublicationAndHasRightsToUpdate(Publication publicationUpdate, URI cristinId,
                                                                          String userName)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(userName)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withPersonCristinId(cristinId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_OWN_RESOURCES)
                   .withTopLevelCristinOrgId(publicationUpdate.getResourceOwner().getOwnerAffiliation())
                   .build();
    }

    private InputStream contributorsUpdatesPublication(Publication publicationUpdate,
                                                       URI cristinId,
                                                       URI topLevelCristinOrgId)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(randomString())
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(randomUri())
                   .withPersonCristinId(cristinId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_OWN_RESOURCES)
                   .withTopLevelCristinOrgId(topLevelCristinOrgId)
                   .build();
    }

    private InputStream contributorUpdatesPublicationWithoutHavingRights(Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream userUpdatesPublicationAndHasRightToUpdate(Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withPersonCristinId(randomUri())
                   .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD, MANAGE_DEGREE)
                   .withTopLevelCristinOrgId(publicationUpdate.getResourceOwner().getOwnerAffiliation())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    protected InputStream ownerUpdatesOwnPublication(SortableIdentifier publicationIdentifier,
                                                     Publication publicationUpdate) throws JsonProcessingException {
        return ownerUpdatesOwnPublication(publicationIdentifier, publicationUpdate, null);
    }

    private InputStream ownerUpdatesOwnPublication(SortableIdentifier publicationIdentifier,
                                                   Publication publicationUpdate,
                                                   URI cristinId)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString());
        var customerId = publicationUpdate.getPublisher().getId();
        var request = new HandlerRequestBuilder<Publication>(restApiMapper)
                          .withUserName(publicationUpdate.getResourceOwner().getOwner().getValue())
                          .withCurrentCustomer(customerId)
                          .withBody(publicationUpdate)
                          .withTopLevelCristinOrgId(publicationUpdate.getResourceOwner().getOwnerAffiliation())
                          .withPersonCristinId(randomUri())
                          .withPathParameters(pathParameters);

        if (nonNull(cristinId)) {
            request.withPersonCristinId(cristinId);
        }

        return request.build();
    }

    private InputStream externalClientUpdatesPublication(SortableIdentifier publicationIdentifier,
                                                         Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                   .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                   .withScope(SCOPES_THIRD_PARTY_PUBLICATION_READ)
                   .withBody(publicationUpdate)
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .withPathParameters(pathParameters)
                   .build();
    }

    private InputStream backendClientUpdatesPublication(SortableIdentifier publicationIdentifier,
                                                        Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                   .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                   .withAuthorizerClaim(SCOPE_CLAIM, BACKEND_SCOPE)
                   .withBody(publicationUpdate)
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .withPathParameters(pathParameters)
                   .build();
    }

    private Publication updateTitle(Publication savedPublication) {
        Publication update = savedPublication.copy().build();
        update.getEntityDescription().setMainTitle(randomString());
        return update;
    }

    private TestAppender createAppenderForLogMonitoring() {
        return LogUtils.getTestingAppenderForRootLogger();
    }

    private ResourceService serviceFailsOnModifyRequestWithRuntimeError() {
        var resourceService = spy(getResourceServiceBuilder().build());
        doThrow(new RuntimeException(SOME_MESSAGE)).when(resourceService).updateResource(any(), any());
        return resourceService;
    }

    private HandlerRequestBuilder<Publication> generateInputStreamMissingPathParameters() throws IOException {
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withBody(createNonDegreePublication())
                   .withHeaders(generateHeaders());
    }

    private Map<String, String> generateHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return headers;
    }

    Publication createNonDegreePublication() {
        var publication = fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES);
        publication.setIdentifier(SortableIdentifier.next());
        return publication;
    }

    private GatewayResponse<Problem> toGatewayResponseProblem() throws JsonProcessingException {
        return restApiMapper.readValue(output.toString(),
                                       PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE);
    }

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    private EntityDescription thesisPublishableEntityDescription() {
        return new EntityDescription.Builder()
                   .withMainTitle(RandomDataGenerator.randomString())
                   .withReference(
                       new Reference.Builder()
                           .withDoi(RandomDataGenerator.randomDoi())
                           .withPublicationInstance(
                               PublicationInstanceBuilder.randomPublicationInstance(DegreeMaster.class))
                           .build())
                   .build();
    }

    private InputStream curatorWithAccessRightsUpdatesPublication(Publication publication, URI customerId,
                                                                  URI topLevelCristinOrgId,
                                                                  AccessRight... accessRights)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publication)
                   .withAccessRights(customerId, accessRights)
                   .withTopLevelCristinOrgId(topLevelCristinOrgId)
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream curatorWithAccessRightsRepublishedPublication(Publication publication, URI customerId,
                                                                      URI topLevelCristinOrgId,
                                                                      AccessRight... accessRights)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString());
        return new HandlerRequestBuilder<RepublishPublicationRequest>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(new RepublishPublicationRequest())
                   .withAccessRights(customerId, accessRights)
                   .withTopLevelCristinOrgId(topLevelCristinOrgId)
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream curatorPublicationOwnerUpdatesPublication(Publication publication)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(publication.getResourceOwner().getOwner().getValue())
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withBody(publication)
                   .withAccessRights(customerId, MANAGE_PUBLISHING_REQUESTS, MANAGE_DOI, SUPPORT, MANAGE_RESOURCES_STANDARD)
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .build();
    }
}
