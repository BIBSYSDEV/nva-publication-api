package no.unit.nva.publication.create;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.IMPORT_PROCESS_WENT_WRONG;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.RESOURCE_IS_NOT_PUBLISHABLE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.ROLLBACK_WENT_WRONG_MESSAGE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.SCOPUS_IDENTIFIER;
import static no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever.ACCEPT;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.create.pia.PiaClientConfig;
import no.unit.nva.publication.create.pia.PiaUpdateRequest;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.secrets.SecretsReader;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

@WireMockTest(httpsEnabled = true)
@ExtendWith(MockitoExtension.class)
class CreatePublicationFromImportCandidateHandlerTest extends ResourcesLocalTest {

    public static final String SOME_SECRETS_KEY_NAME = "some-secrets-key-name";
    public static final String SOME_USERNAME_KEY = "some-username-key";
    public static final String SOME_PIA_PASSWORD_KEY = "some-pia-password-key";
    public static final String SOME_PERSISTED_BUCKET = "some-persisted-bucket";
    public static final String SOME_CANDIDATE_BUCKET = "some-candidate-bucket";
    public static final String IMPORT_CANDIDATES_TABLE = "import-candidates-table";
    public static final String PUBLICATIONS_TABLE = "publications-table";
    private ByteArrayOutputStream output;
    private Context context;
    private ResourceService importCandidateService;
    private ResourceService publicationService;
    private CreatePublicationFromImportCandidateHandler handler;
    private S3Client s3Client;

    private ImportCandidateHandlerConfigs configs;
    private PiaClientConfig piaClientConfig;

    @BeforeEach
    public void setUp(@Mock Context context, @Mock S3Client s3Client,
                      WireMockRuntimeInfo wireMockRuntimeInfo) {
        this.s3Client = s3Client;
        super.init(IMPORT_CANDIDATES_TABLE, PUBLICATIONS_TABLE);
        importCandidateService = new ResourceService(client, IMPORT_CANDIDATES_TABLE);
        publicationService = new ResourceService(client, PUBLICATIONS_TABLE);
        this.context = context;
        output = new ByteArrayOutputStream();
        piaClientConfig = createPiaConfig(wireMockRuntimeInfo);
        configs = new ImportCandidateHandlerConfigs(SOME_PERSISTED_BUCKET,
                                                    SOME_CANDIDATE_BUCKET,
                                                    importCandidateService,
                                                    publicationService,
                                                    s3Client,
                                                    piaClientConfig);
        handler = new CreatePublicationFromImportCandidateHandler(configs);
        mockPostAuidWriting();
    }

    @Test
    void shouldReturnPublicationResponseWhenPublicationHasBeenCreated() throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        var publication = publicationService.getPublicationByIdentifier(getBodyObject(response).getIdentifier());
        var updatedImportCandidate = importCandidateService.getImportCandidateByIdentifier(
            importCandidate.getIdentifier());

        assertThat(updatedImportCandidate.getImportStatus().candidateStatus(),
                   is(equalTo(CandidateStatus.IMPORTED)));
        assertThat(publication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldCreatePublicationWithValuesFromRequestBodyAndNotPersistedImportCandidate()
        throws NotFoundException, IOException {
        var persistedImportCandidate = createPersistedImportCandidate();
        var importCandidateRequestBody = persistedImportCandidate.copyImportCandidate().withDoi(randomDoi()).build();
        var request = createRequest(importCandidateRequestBody);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        var publication = publicationService.getPublicationByIdentifier(getBodyObject(response).getIdentifier());
        var updatedImportCandidate = importCandidateService
                                         .getImportCandidateByIdentifier(persistedImportCandidate.getIdentifier());

        assertThat(updatedImportCandidate.getDoi(), is(equalTo(persistedImportCandidate.getDoi())));
        assertThat(publication.getDoi(), is(equalTo(importCandidateRequestBody.getDoi())));
    }

    @Test
    void shouldCopyAssociatedResourceFiles() throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        var artifactId = ((File) importCandidate.getAssociatedArtifacts().stream().findFirst().get()).getIdentifier()
                             .toString();

        handler.handleRequest(request, output, context);

        verify(s3Client, atLeastOnce()).copyObject(
            CopyObjectRequest.builder()
                .sourceBucket(SOME_CANDIDATE_BUCKET)
                .sourceKey(artifactId)
                .destinationBucket(SOME_PERSISTED_BUCKET)
                .destinationKey(artifactId)
                .build());
    }

    @Test
    void shouldReturnBadGatewayAndNotUpdateBothResourcesWhenPublicationPersistenceFails(
        @Mock ResourceService resourceService, WireMockRuntimeInfo wireMockRuntimeInfo)
        throws IOException, ApiGatewayException {

        publicationService = resourceService;
        configs = new ImportCandidateHandlerConfigs(SOME_PERSISTED_BUCKET,
                                                    SOME_CANDIDATE_BUCKET,
                                                    importCandidateService,
                                                    publicationService,
                                                    s3Client,
                                                    piaClientConfig);
        handler = new CreatePublicationFromImportCandidateHandler(configs);
        when(publicationService.autoImportPublication(any())).thenThrow(
            new TransactionFailedException(new Exception()));
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var notUpdatedImportCandidate = importCandidateService.getImportCandidateByIdentifier(
            importCandidate.getIdentifier());

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_GATEWAY)));
        assertThat(notUpdatedImportCandidate.getImportStatus().candidateStatus(),
                   is(equalTo(CandidateStatus.NOT_IMPORTED)));
        assertThat(response.getBodyObject(Problem.class).getDetail(), containsString(IMPORT_PROCESS_WENT_WRONG));
    }

    @Test
    void shouldReturnBadGatewayWhenImportCandidatePersistenceFails(@Mock ResourceService resourceService,
                                                                   WireMockRuntimeInfo wireMockRuntimeInfo)
        throws IOException, ApiGatewayException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);

        importCandidateService = resourceService;
        configs = new ImportCandidateHandlerConfigs(SOME_PERSISTED_BUCKET,
                                                    SOME_CANDIDATE_BUCKET,
                                                    importCandidateService,
                                                    publicationService,
                                                    s3Client,
                                                    piaClientConfig
        );
        handler = new CreatePublicationFromImportCandidateHandler(configs);
        when(importCandidateService.updateImportStatus(any(), any()))
            .thenThrow(new TransactionFailedException(new Exception()));

        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_GATEWAY)));
    }

    @Test
    void shouldReturnBadGatewayWhenCanNotAccessImportCandidate()
        throws IOException {
        var importCandidate = createImportCandidate();
        importCandidateService = new ResourceService(client, Clock.systemDefaultZone());
        var request = createRequest(importCandidate);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_GATEWAY)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserHasNoValidAccessRights() throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequestWithoutAccessRights(importCandidate);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnBadGatewayWhenRollbackFails(@Mock ResourceService resourceService)
        throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        publicationService = resourceService;
        importCandidateService = resourceService;
        configs = new ImportCandidateHandlerConfigs(SOME_PERSISTED_BUCKET,
                                                    SOME_CANDIDATE_BUCKET,
                                                    importCandidateService,
                                                    publicationService,
                                                    s3Client,
                                                    piaClientConfig);
        handler = new CreatePublicationFromImportCandidateHandler(configs);
        when(importCandidateService.updateImportStatus(any(), any()))
            .thenCallRealMethod()
            .thenThrow(new NotFoundException(""));

        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_GATEWAY)));
        assertThat(response.getBodyObject(Problem.class).getDetail(), containsString(ROLLBACK_WENT_WRONG_MESSAGE));
    }

    @Test
    void shouldReturnBadRequestWhenImportCandidateHasStatusImported() throws IOException,
                                                                             NotFoundException {
        var importCandidate = createImportedPersistedImportCandidate();
        var request = createRequest(importCandidate);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getBodyObject(Problem.class).getDetail(),
                   containsString(RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE));
    }

    @Test
    void shouldReturnBadRequestWhenImportCandidateIsMissingScopusIdentifier() throws IOException,
                                                                                     NotFoundException {
        var importCandidate = createImportCandidateWithoutScopusId();
        var request = createRequest(importCandidate);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getBodyObject(Problem.class).getDetail(),
                   containsString(RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE));
    }

    @Test
    void shouldCreateNvaResourceBasedOnUserInput() throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var userInput = importCandidate.copyImportCandidate().build();
        var userInputContributor = randomContributor();
        userInput.getEntityDescription().setContributors(List.of(userInputContributor));
        var request = createRequest(userInput);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        var publication = publicationService.getPublicationByIdentifier(getBodyObject(response).getIdentifier());
        assertThat(publication.getEntityDescription().getContributors(),
                   hasItem(samePropertyValuesAs(userInputContributor)));
    }

    @Test
    void shouldOnlyCopyFilesThatWhereKeptByImporter() throws NotFoundException, IOException {
        var fileKeptByImporter = randomFile();
        var fileNotKeptByImporter = randomFile();
        var fileAddedByImporter = randomFile();
        var importCandidateAssociatedArtifactList = new AssociatedArtifactList(fileKeptByImporter,
                                                                               fileNotKeptByImporter);
        var importCandidate = createPersistedImportCandidate(importCandidateAssociatedArtifactList);
        var userInput = importCandidate
                            .copyImportCandidate()
                            .withAssociatedArtifacts(
                                new AssociatedArtifactList(fileKeptByImporter, fileAddedByImporter))
                            .build();
        var request = createRequest(userInput);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        var publication = publicationService.getPublicationByIdentifier(getBodyObject(response).getIdentifier());
        assertThat(publication.getAssociatedArtifacts(), hasItem(fileKeptByImporter));
        assertThat(publication.getAssociatedArtifacts(), hasItem(fileAddedByImporter));
        assertThat(publication.getAssociatedArtifacts(), not(hasItem(fileNotKeptByImporter)));
        verify(s3Client, atLeastOnce()).copyObject(
            CopyObjectRequest.builder()
                .sourceBucket(SOME_CANDIDATE_BUCKET)
                .sourceKey(fileKeptByImporter.getIdentifier().toString())
                .destinationBucket(SOME_PERSISTED_BUCKET)
                .destinationKey(fileKeptByImporter.getIdentifier().toString())
                .build());
        verify(s3Client, never()).copyObject(
            CopyObjectRequest.builder()
                .sourceBucket(SOME_CANDIDATE_BUCKET)
                .sourceKey(fileNotKeptByImporter.getIdentifier().toString())
                .destinationBucket(SOME_PERSISTED_BUCKET)
                .destinationKey(fileNotKeptByImporter.getIdentifier().toString())
                .build());
    }

    @Test
    void shouldThrowBadRequestExceptionWhenTryingToImportCandideWithoutTitle()
        throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var userInput = importCandidate.copyImportCandidate().build();
        userInput.getEntityDescription().setMainTitle(null);
        var request = createRequest(userInput);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getBodyObject(Problem.class).getDetail(),
                   containsString(RESOURCE_IS_NOT_PUBLISHABLE));
    }

    @Test
    void publishedDateShoulBeSetToTimeWhenPublicationEntersDatabase() throws NotFoundException,
                                                                             IOException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        var start = Instant.now();
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        var publication = publicationService.getPublicationByIdentifier(getBodyObject(response).getIdentifier());
        assertThat(publication.getCreatedDate(), is(equalTo(publication.getPublishedDate())));
        assertThat(publication.getCreatedDate(), is(equalTo(publication.getModifiedDate())));
        assertThat(publication.getCreatedDate(), is(greaterThan(start)));
    }

    @Test
    void shouldWriteToPiaWhenCristinIdHaveBeenUpdatedByUser() throws NotFoundException, IOException {
        var auid = randomString();
        var contributorWithAuid = createContributorWithAuid(auid, 1);
        var importCandidate = createPersistedImportCandidate(List.of(contributorWithAuid));
        var cristinId = randomUri();
        var contributorUpdatedWithCristinId = updateContributorWithCristinId(contributorWithAuid, cristinId);
        var userInput = importCandidate.copy().build();
        userInput.getEntityDescription().setContributors(List.of(contributorUpdatedWithCristinId));
        var request = createRequest(importCandidate);
        var expectedBody = List.of(PiaUpdateRequest.toPiaRequest(contributorUpdatedWithCristinId,
                                                                 extractScopusId(importCandidate)));
        handler.handleRequest(request, output, context);
        WireMock.verify(postRequestedFor(urlEqualTo("/sentralimport/authors"))
                            .withRequestBody(WireMock.equalTo(expectedBody.toString())));
    }

    @Test
    void shouldWriteToPiaWithSeveralContributorsAtOnce () throws NotFoundException, IOException {
        var auid1 = randomString();
        var contributorWithAuid1 = createContributorWithAuid(auid1, 1);
        var auid2 = randomString();
        var contributorWithAuid2 = createContributorWithAuid(auid2, 2);
        var importCandidate = createPersistedImportCandidate(List.of(contributorWithAuid1, contributorWithAuid2));
        var cristinId1 = randomUri();
        var cristinId2 = randomUri();
        var contributorUpdatedWithCristinId1 = updateContributorWithCristinId(contributorWithAuid1, cristinId1);
        var contributorUpdatedWithCristinId2 = updateContributorWithCristinId(contributorWithAuid2, cristinId2);
        var userInput = importCandidate.copy().build();
        userInput.getEntityDescription().setContributors(List.of(contributorUpdatedWithCristinId1,
                                                                 contributorUpdatedWithCristinId2));
        var request = createRequest(importCandidate);
        handler.handleRequest(request, output, context);
        var expectedNumberOfTimesPostRequestIsSent = 1;
        var expectedBody = List.of(PiaUpdateRequest.toPiaRequest(contributorUpdatedWithCristinId1,
                                                                 extractScopusId(importCandidate)),
                                   PiaUpdateRequest.toPiaRequest(contributorUpdatedWithCristinId2,
                                                                 extractScopusId(importCandidate)));
        WireMock.verify(expectedNumberOfTimesPostRequestIsSent,
                        postRequestedFor(urlEqualTo("/sentralimport/authors"))
                            .withRequestBody(WireMock.equalTo(expectedBody.toString())));
    }

    @Test
    void shouldContinueImportingEvenWhenPiaRestRespondsWithErrorCodes()
        throws NotFoundException, IOException {
        var auid = randomString();
        var contributorWithAuid = createContributorWithAuid(auid, 1);
        var importCandidate = createPersistedImportCandidate(List.of(contributorWithAuid));
        var cristinId = randomUri();
        var contributorUpdatedWithCristinId = updateContributorWithCristinId(contributorWithAuid, cristinId);
        var userInput = importCandidate.copy().build();
        userInput.getEntityDescription().setContributors(List.of(contributorUpdatedWithCristinId));
        var request = createRequest(importCandidate);
        mockBadRequestResponseFromPia();
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    private static PublicationResponse getBodyObject(GatewayResponse<PublicationResponse> response)
        throws JsonProcessingException {
        return response.getBodyObject(PublicationResponse.class);
    }

    private String extractScopusId(ImportCandidate importCandidate) {
        return
            importCandidate.getAdditionalIdentifiers()
                .stream()
                .filter(additionalIdentifier -> "scopus".equalsIgnoreCase(additionalIdentifier.getSourceName()))
                .map(
                    AdditionalIdentifier::getValue).findFirst().get();
    }

    private PiaClientConfig createPiaConfig(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return new PiaClientConfig(wireMockRuntimeInfo.getHttpBaseUrl(),
                                   SOME_USERNAME_KEY,
                                   SOME_PIA_PASSWORD_KEY,
                                   SOME_SECRETS_KEY_NAME,
                                   WiremockHttpClient.create(),
                                   setupPiaSecrets());
    }

    private SecretsReader setupPiaSecrets() {
        var fakeSecretsManagerClient = new FakeSecretsManagerClient();
        fakeSecretsManagerClient.putSecret(SOME_SECRETS_KEY_NAME, SOME_USERNAME_KEY, randomString());
        fakeSecretsManagerClient.putSecret(SOME_SECRETS_KEY_NAME, SOME_PIA_PASSWORD_KEY, randomString());
        return new SecretsReader(fakeSecretsManagerClient);
    }

    private void mockBadRequestResponseFromPia() {
        stubFor(WireMock.post(urlMatching("/sentralimport/authors"))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)));
    }

    private void mockPostAuidWriting() {
        stubFor(WireMock.post(urlMatching("/sentralimport/authors"))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_CREATED)));
    }

    private Contributor updateContributorWithCristinId(Contributor contributorWithAuid, URI cristinId) {
        var identityWithCristinId = new Identity.Builder()
                                        .withId(cristinId)
                                        .withAdditionalIdentifiers(contributorWithAuid.getIdentity()
                                                                       .getAdditionalIdentifiers())
                                        .withName(randomString())
                                        .build();
        return contributorWithAuid.copy()
                   .withIdentity(identityWithCristinId)
                   .build();
    }

    private Contributor createContributorWithAuid(String auid, int sequence) {
        return new Contributor.Builder()
                   .withSequence(sequence)
                   .withIdentity(identityWithAuid(auid))
                   .withRole(new RoleType(Role.CREATOR))
                   .withAffiliations(List.of(randomAffiliation()))
                   .build();
    }

    private Identity identityWithAuid(String auid) {
        return new Identity.Builder()
                   .withName(randomString())
                   .withAdditionalIdentifiers(List.of(additionalIdentifierFromAuid(auid)))
                   .build();
    }

    private AdditionalIdentifier additionalIdentifierFromAuid(String auid) {
        return new AdditionalIdentifier("scopus-auid", auid);
    }

    private Organization randomAffiliation() {
        return new Organization.Builder()
                   .withId(randomUri())
                   .build();
    }

    private PublishedFile randomFile() {
        return new PublishedFile(UUID.randomUUID(),
                                 randomString(),
                                 "pdf",
                                 12312L,
                                 null,
                                 false,
                                 false,
                                 null,
                                 null,
                                 null);
    }

    private ImportCandidate createImportCandidateWithoutScopusId() throws NotFoundException {
        var candidate = createImportCandidate();
        candidate.setAdditionalIdentifiers(Set.of());
        var importCandidate = importCandidateService.persistImportCandidate(candidate);
        return importCandidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private ImportCandidate createImportedPersistedImportCandidate() throws NotFoundException {
        var candidate = createImportCandidate();
        candidate.setImportStatus(ImportStatusFactory.createImported(randomPerson(), randomUri()));
        var importCandidate = importCandidateService.persistImportCandidate(candidate);
        return importCandidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private ImportCandidate createPersistedImportCandidate(List<Contributor> contributors)
        throws NotFoundException {
        var candidate = createImportCandidate();
        candidate.getEntityDescription().setContributors(contributors);
        var importCandidate = importCandidateService.persistImportCandidate(candidate);
        return importCandidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private Username randomPerson() {
        return new Username(randomString());
    }

    private InputStream createRequestWithoutAccessRights(ImportCandidate importCandidate) throws
                                                                                          JsonProcessingException {
        var headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        var user = UserInstance.create(randomString(), importCandidate.getPublisher().getId());
        return new HandlerRequestBuilder<ImportCandidate>(restApiMapper)
                   .withHeaders(headers)
                   .withBody(importCandidate)
                   .withCurrentCustomer(user.getOrganizationUri())
                   .build();
    }

    private InputStream createRequest(ImportCandidate importCandidate) throws JsonProcessingException {
        var headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        var user = UserInstance.create(randomString(), importCandidate.getPublisher().getId());
        return new HandlerRequestBuilder<ImportCandidate>(restApiMapper)
                   .withHeaders(headers)
                   .withUserName(randomString())
                   .withBody(importCandidate)
                   .withCurrentCustomer(user.getOrganizationUri())
                   .withAccessRights(user.getOrganizationUri(), AccessRight.PROCESS_IMPORT_CANDIDATE.name())
                   .build();
    }

    private ImportCandidate createPersistedImportCandidate() throws NotFoundException {
        var candidate = createImportCandidate();
        var importCandidate = importCandidateService.persistImportCandidate(candidate);
        return importCandidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private ImportCandidate createPersistedImportCandidate(AssociatedArtifactList associatedArtifacts)
        throws NotFoundException {
        var candidate = createImportCandidate();
        candidate.setAssociatedArtifacts(associatedArtifacts);
        var importCandidate = importCandidateService.persistImportCandidate(candidate);
        return importCandidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private ImportCandidate createImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription())
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
                   .withFundings(List.of(new FundingBuilder().build()))
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(SCOPUS_IDENTIFIER, randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of(File.builder()
                                                        .withName("some_name.pdf")
                                                        .withMimeType("application/pdf")
                                                        .withSize(123L)
                                                        .withIdentifier(UUID.randomUUID())
                                                        .withLicense(URI.create("https://hei"))
                                                        .withPublisherAuthority(true)
                                                        .buildPublishedFile()))
                   .build();
    }

    private EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder()
                   .withPublicationDate(new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withSequence(1)
                   .build();
    }
}
