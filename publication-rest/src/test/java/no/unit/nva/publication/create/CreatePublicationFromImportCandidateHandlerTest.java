package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.IMPORT_CANDIDATES_TABLE;
import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.PUBLICATIONS_TABLE;
import static no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever.ACCEPT;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
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
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.ImportStatus;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class CreatePublicationFromImportCandidateHandlerTest extends ResourcesLocalTest {

    private ByteArrayOutputStream output;
    private Context context;
    private ResourceService importCandidateService;
    private ResourceService publicationService;
    private CreatePublicationFromImportCandidateHandler handler;

    @BeforeEach
    public void setUp() {
        super.init(IMPORT_CANDIDATES_TABLE, PUBLICATIONS_TABLE);
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        importCandidateService = new ResourceService(client, Clock.systemDefaultZone(), SortableIdentifier::next,
                                                     IMPORT_CANDIDATES_TABLE);
        publicationService = new ResourceService(client, Clock.systemDefaultZone(), SortableIdentifier::next);
        context = mock(Context.class);
        output = new ByteArrayOutputStream();
        handler = new CreatePublicationFromImportCandidateHandler(importCandidateService, publicationService);
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

        assertThat(updatedImportCandidate.getImportStatus(), is(equalTo(ImportStatus.IMPORTED)));
        assertThat(publication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldReturnBadGatewayAndNotUpdateBothResourcesWhenPublicationPersistenceFails()
        throws IOException, ApiGatewayException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        publicationService = mock(ResourceService.class);
        handler = new CreatePublicationFromImportCandidateHandler(importCandidateService, publicationService);
        when(publicationService.autoImportPublication(any())).thenThrow(new TransactionFailedException(new Exception()));
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var notUpdatedImportCandidate = importCandidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(notUpdatedImportCandidate.getImportStatus(), is(equalTo(ImportStatus.NOT_IMPORTED)));
    }

    @Test
    void shouldReturnBadGatewayWhenImportCandidatePersistenceFails()
        throws IOException, ApiGatewayException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate);
        importCandidateService = mock(ResourceService.class);
        handler = new CreatePublicationFromImportCandidateHandler(importCandidateService, publicationService);
        when(importCandidateService.updateImportStatus(any(), any())).thenThrow(new TransactionFailedException(new Exception()));
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
    }

    @Test
    void shouldReturnNotFoundWhenCanNotAccessImportCandidate()
        throws IOException {
        var importCandidate = createImportCandidate();
        importCandidateService = new ResourceService(client, Clock.systemDefaultZone());
        var request = createRequest(importCandidate);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    private static PublicationResponse getBodyObject(GatewayResponse<PublicationResponse> response)
        throws JsonProcessingException {
        return response.getBodyObject(PublicationResponse.class);
    }

    private InputStream createRequest(ImportCandidate importCandidate) throws JsonProcessingException {
        var headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        return new HandlerRequestBuilder<ImportCandidate>(restApiMapper)
                   .withHeaders(headers)
                   .withBody(importCandidate)
                   .build();
    }

    private ImportCandidate createPersistedImportCandidate() throws NotFoundException {
        var candidate = createImportCandidate();
        var importCandidate = importCandidateService.persistImportCandidate(candidate);
        return importCandidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private ImportCandidate createImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatus.NOT_IMPORTED)
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
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
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
