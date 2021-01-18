package no.unit.nva.publication.delete;

import static java.util.Collections.singletonMap;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.service.PublicationsDynamoDBLocal;
import no.unit.nva.publication.service.impl.DynamoDBPublicationService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

@EnableRuleMigrationSupport
public class DeletePublicationHandlerTest {

    public static final String IDENTIFIER = "identifier";
    public static final String WILDCARD = "*";
    public static final String SOME_OWNER = "some_owner";
    public static final String REQUEST_CONTEXT = "requestContext";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    private DeletePublicationHandler handler;
    private PublicationService publicationService;
    private Environment environment;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    @Rule
    public PublicationsDynamoDBLocal db = new PublicationsDynamoDBLocal();

    @BeforeEach
    public void setUp() {
        prepareEnvironment();
        publicationService = new DynamoDBPublicationService(
                objectMapper,
                db.getTable(),
                db.getByPublisherIndex(),
                db.getByPublishedDateIndex()
        );
        handler = new DeletePublicationHandler(publicationService, environment);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    private void prepareEnvironment() {
        environment = Mockito.mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(DynamoDBPublicationService.TABLE_NAME_ENV))
                .thenReturn(PublicationsDynamoDBLocal.NVA_RESOURCES_TABLE_NAME);
        when(environment.readEnv(DynamoDBPublicationService.BY_PUBLISHER_INDEX_NAME_ENV))
                .thenReturn(PublicationsDynamoDBLocal.BY_PUBLISHER_INDEX_NAME);
        when(environment.readEnv(DynamoDBPublicationService.BY_PUBLISHED_PUBLICATIONS_INDEX_NAME))
                .thenReturn(PublicationsDynamoDBLocal.BY_PUBLISHED_DATE_INDEX_NAME);
    }

    @Test
    void handleRequestReturnsAcceptedWhenOnDraftPublication() throws IOException, ApiGatewayException {
        Publication publication = PublicationGenerator.publicationWithoutIdentifier();
        publicationService.createPublication(publication);

        InputStream inputStream = new HandlerRequestBuilder<Publication>(objectMapper)
                .withHeaders(TestHeaders.getRequestHeaders())
                .withPathParameters(singletonMap(IDENTIFIER, publication.getIdentifier().toString()))
                .withOtherProperties(getClaims(publication.getOwner()))
                .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Void> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(HttpStatus.SC_ACCEPTED, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsNotImplementedWhenOnPublishedPublication() throws IOException, ApiGatewayException {
        Publication publication = PublicationGenerator.publicationWithoutIdentifier();
        publication.setStatus(PublicationStatus.PUBLISHED);
        publicationService.createPublication(publication);

        InputStream inputStream = new HandlerRequestBuilder<Publication>(objectMapper)
                .withHeaders(TestHeaders.getRequestHeaders())
                .withPathParameters(singletonMap(IDENTIFIER, publication.getIdentifier().toString()))
                .withOtherProperties(getClaims(publication.getOwner()))
                .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsNotFoundWhenNonExistingPublication() throws IOException {
        UUID identifier = UUID.randomUUID();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(objectMapper)
                .withHeaders(TestHeaders.getRequestHeaders())
                .withPathParameters(singletonMap(IDENTIFIER, identifier.toString()))
                .withOtherProperties(getClaims(SOME_OWNER))
                .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(HttpStatus.SC_NOT_FOUND, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsNotFoundWhenCallerIsNotOwnerOfPublication() throws IOException, ApiGatewayException {
        Publication publication = PublicationGenerator.publicationWithoutIdentifier();
        publicationService.createPublication(publication);

        InputStream inputStream = new HandlerRequestBuilder<Publication>(objectMapper)
                .withHeaders(TestHeaders.getRequestHeaders())
                .withPathParameters(singletonMap(IDENTIFIER, publication.getIdentifier().toString()))
                .withOtherProperties(getClaims(SOME_OWNER))
                .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(HttpStatus.SC_NOT_FOUND, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsNotImplementedWhenAlreadyMarkedForDeletionPublication()
            throws IOException, ApiGatewayException {
        Publication publication = PublicationGenerator.publicationWithoutIdentifier();
        publication.setStatus(PublicationStatus.DRAFT_FOR_DELETION);
        publicationService.createPublication(publication);

        InputStream inputStream = new HandlerRequestBuilder<Publication>(objectMapper)
                .withHeaders(TestHeaders.getRequestHeaders())
                .withPathParameters(singletonMap(IDENTIFIER, publication.getIdentifier().toString()))
                .withOtherProperties(getClaims(publication.getOwner()))
                .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, gatewayResponse.getStatusCode());
    }

    private Map<String, Object> getClaims(String owner) {
        return Map.of(
            REQUEST_CONTEXT, Map.of(
            AUTHORIZER, Map.of(
            CLAIMS, Map.of(
            CUSTOM_FEIDE_ID, owner))));
    }

}
