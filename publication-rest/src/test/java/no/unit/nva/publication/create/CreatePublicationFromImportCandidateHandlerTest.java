package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.IMPORT_CANDIDATES_TABLE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.IMPORT_PROCESS_WENT_WRONG;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.PUBLICATIONS_TABLE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.ROLLBACK_WENT_WRONG_MESSAGE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.SCOPUS_IDENTIFIER;
import static no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever.ACCEPT;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.CandidateStatus;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

@ExtendWith(MockitoExtension.class)
class CreatePublicationFromImportCandidateHandlerTest extends ResourcesLocalTest {

    private static final String NVA_PERSISTED_STORAGE_BUCKET_NAME_ENV = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    public static final String IMPORT_CANDIDATES_STORAGE_BUCKET_ENV = "IMPORT_CANDIDATES_STORAGE_BUCKET";
    private ByteArrayOutputStream output;
    private Context context;
    private ResourceService importCandidateService;
    private ResourceService publicationService;
    private CreatePublicationFromImportCandidateHandler handler;
    private S3Client s3Client;

    private static PublicationResponse getBodyObject(GatewayResponse<PublicationResponse> response)
        throws JsonProcessingException {
        return response.getBodyObject(PublicationResponse.class);
    }

    @BeforeEach
    public void setUp(@Mock Environment environment, @Mock Context context, @Mock S3Client s3Client) {
        this.s3Client = s3Client;
        super.init(IMPORT_CANDIDATES_TABLE, PUBLICATIONS_TABLE);
        lenient().when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        lenient().when(environment.readEnv(IMPORT_CANDIDATES_STORAGE_BUCKET_ENV)).thenReturn("some-candidate-bucket");
        lenient().when(environment.readEnv(NVA_PERSISTED_STORAGE_BUCKET_NAME_ENV)).thenReturn("some-persisted-bucket");
        importCandidateService = new ResourceService(client, IMPORT_CANDIDATES_TABLE);
        publicationService = new ResourceService(client, PUBLICATIONS_TABLE);
        this.context = context;
        output = new ByteArrayOutputStream();
        handler = new CreatePublicationFromImportCandidateHandler(importCandidateService, publicationService, s3Client);
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
                .sourceBucket("some-candidate-bucket")
                .sourceKey(artifactId)
                .destinationBucket("some-persisted-bucket")
                .destinationKey(artifactId)
                .build());
    }

    @Test
    void shouldReturnBadGatewayAndNotUpdateBothResourcesWhenPublicationPersistenceFails(@Mock
                                                                                        ResourceService resourceService)
        throws IOException, ApiGatewayException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        publicationService = resourceService;
        handler = new CreatePublicationFromImportCandidateHandler(importCandidateService, publicationService, s3Client);
        when(publicationService.autoImportPublication(any())).thenThrow(
            new TransactionFailedException(new Exception()));
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
    void shouldReturnBadGatewayWhenImportCandidatePersistenceFails(@Mock ResourceService resourceService)
        throws IOException, ApiGatewayException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        importCandidateService = resourceService;
        handler = new CreatePublicationFromImportCandidateHandler(importCandidateService, publicationService, s3Client);
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
        handler = new CreatePublicationFromImportCandidateHandler(importCandidateService, publicationService, s3Client);
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
                   .build();
    }
}
