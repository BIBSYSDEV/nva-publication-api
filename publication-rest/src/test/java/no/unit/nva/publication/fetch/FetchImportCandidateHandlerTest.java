package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.model.testing.ImportCandidateGenerator.randomImportCandidate;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.fetch.FetchImportCandidateHandler.IMPORT_CANDIDATE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ENV_NAME_NVA_FRONTEND_DOMAIN;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;

@ExtendWith(MockitoExtension.class)
@WireMockTest(httpsEnabled = true)
public class FetchImportCandidateHandlerTest extends ResourcesLocalTest {

    public static final String IDENTIFIER = "importCandidateIdentifier";
    private ByteArrayOutputStream output;
    private final Context context = new FakeContext();
    private ResourceService resourceService;
    private FetchImportCandidateHandler handler;

    @BeforeEach
    public void setUp(@Mock Environment environment) {
        super.init();
        lenient().when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        lenient().when(environment.readEnv(ENV_NAME_NVA_FRONTEND_DOMAIN)).thenReturn("localhost");
        resourceService = getResourceService(client);
        output = new ByteArrayOutputStream();
        handler = new FetchImportCandidateHandler(resourceService, new Environment());
    }

    @Test
    void shouldReturnImportCandidateSuccessfullyWhenImportCandidateIsInDatabase()
        throws NotFoundException, IOException {
        var importCandidate = createPersistedImportCandidate();
        var request = createRequest(importCandidate.getIdentifier());
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, ImportCandidate.class);
        var responseImportCandidate = response.getBodyObject(ImportCandidate.class);
        assertThat(importCandidate, is(equalTo(responseImportCandidate)));
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
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        return resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }
}
