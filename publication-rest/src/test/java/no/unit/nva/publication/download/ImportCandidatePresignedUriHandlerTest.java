package no.unit.nva.publication.download;

import static java.util.UUID.randomUUID;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.download.ImportCandidatePresignedUrlHandler.IMPORT_CANDIDATE_MISSES_FILE_EXCEPTION_MESSAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class ImportCandidatePresignedUriHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = new FakeContext();
    private ResourceService importCandidateService;
    private S3Presigner s3Presigner;
    private ImportCandidatePresignedUrlHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        super.init("import-candidates-table");
        importCandidateService = getResourceServiceBuilder(client)
                                     .withTableName("import-candidates-table")
                                     .build();
        s3Presigner = mock(S3Presigner.class);
        handler = new ImportCandidatePresignedUrlHandler(s3Presigner, importCandidateService);
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnNotFoundWhenImportCandidateDoesNotExist() throws IOException {
        var request = createRequestForCandidateWithFile(SortableIdentifier.next(), randomUUID());
        handler.handleRequest(request, output, CONTEXT);
        var problem = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(problem.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnNotFoundWhenFileDoesNotExist() throws IOException {
        var importCandidate = importCandidateService.persistImportCandidate(randomImportCandidate());
        var nonExistingFileIdentifier = randomUUID();

        var request = createRequestForCandidateWithFile(importCandidate.getIdentifier(), nonExistingFileIdentifier);
        handler.handleRequest(request, output, CONTEXT);

        var problem = GatewayResponse.fromOutputStream(output, Problem.class);
        var detail = problem.getBodyObject(Problem.class).getDetail();

        assertThat(problem.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
        assertThat(detail, is(equalTo(String.format(IMPORT_CANDIDATE_MISSES_FILE_EXCEPTION_MESSAGE,
                                               importCandidate.getIdentifier(), nonExistingFileIdentifier))));
    }

    @Test
    void shouldReturnPresignedUrlSuccessfully() throws IOException, URISyntaxException {
        var importCandidate = importCandidateService.persistImportCandidate(randomImportCandidate());
        var file = (File) importCandidate.getAssociatedArtifacts().getFirst();
        var request = createRequestForCandidateWithFile(importCandidate.getIdentifier(), file.getIdentifier());
        var expectedPresignedUri = mockS3Presigner(file);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, PresignedUri.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getBodyObject(PresignedUri.class), is(equalTo(expectedPresignedUri)));

    }

    private PresignedUri mockS3Presigner(File file) throws MalformedURLException, URISyntaxException {
        var request = mock(PresignedGetObjectRequest.class);
        var presignedUrl = randomUri().toURL();
        when(request.url()).thenReturn(presignedUrl);
        var expires = Instant.now();
        when(request.expiration()).thenReturn(expires);
        when(s3Presigner.presignGetObject((GetObjectPresignRequest) any())).thenReturn(request);
        return PresignedUri.builder()
                   .withFileIdentifier(file.getIdentifier())
                   .withSignedUri(presignedUrl.toURI())
                   .withExpiration(expires)
                   .build();
    }

    private static InputStream createRequestForCandidateWithFile(SortableIdentifier importCandidateIdentifier,
                                                                 UUID fileIdentifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(restApiMapper)
                                      .withPathParameters(Map.of(
                                          "importCandidateIdentifier", importCandidateIdentifier.toString(),
                                          "fileIdentifier", fileIdentifier.toString()
                                      ))
                                      .build();
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder().withImportStatus(ImportStatusFactory.createNotImported())
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
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Scopus", randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of(File.builder().withIdentifier(randomUUID()).buildOpenFile()))
                   .build();
    }

    private EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder().withPublicationDate(
                new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private Contributor randomContributor() {
        return new Contributor.Builder().withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }
}