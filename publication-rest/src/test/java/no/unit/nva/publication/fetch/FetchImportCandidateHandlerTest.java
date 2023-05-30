package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.fetch.FetchImportCandidateHandler.IMPORT_CANDIDATE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ENV_NAME_NVA_FRONTEND_DOMAIN;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.ImportStatus;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

@WireMockTest(httpsEnabled = true)
public class FetchImportCandidateHandlerTest extends ResourcesLocalTest {

    public static final String IDENTIFIER = "importCandidateIdentifier";
    private ByteArrayOutputStream output;
    private Context context;
    private ResourceService resourceService;
    private FetchImportCandidateHandler handler;

    @BeforeEach
    public void setUp() {
        super.init();
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv(ENV_NAME_NVA_FRONTEND_DOMAIN)).thenReturn("localhost");
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        context = mock(Context.class);
        output = new ByteArrayOutputStream();
        handler = new FetchImportCandidateHandler(resourceService);
    }

    @Test
    void shouldReturnImportCandidateSuccessfully() throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate.getIdentifier());
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, ImportCandidate.class);
        var responseImportCandidate = response.getBodyObject(ImportCandidate.class);

        assertThat(importCandidate.getIdentifier(), is(equalTo(responseImportCandidate.getIdentifier())));
    }

    @Test
    void shouldReturnNotFoundWhenImportCandidateDoesNotExist() throws IOException {
        var request = createRequest(SortableIdentifier.next());
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var detail = response.getBodyObject(Problem.class).getDetail();

        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
        assertThat(detail, containsString(IMPORT_CANDIDATE_NOT_FOUND_MESSAGE));
    }

    private InputStream createRequest(SortableIdentifier identifier) throws JsonProcessingException {
        var pathParameters = Map.of(IDENTIFIER, identifier.toString());
        var headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .build();
    }

    private ImportCandidate createPersistedImportCandidate() throws NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(createImportCandidate());
        return resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
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
