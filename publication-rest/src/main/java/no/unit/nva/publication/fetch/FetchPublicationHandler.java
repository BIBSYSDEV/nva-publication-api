package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.common.net.MediaType.ANY_TEXT_TYPE;
import static com.google.common.net.MediaType.HTML_UTF_8;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.net.MediaType.XHTML_UTF_8;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static nva.commons.apigateway.MediaTypes.APPLICATION_DATACITE_XML;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.apigateway.MediaTypes.SCHEMA_ORG;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.doi.DataCiteMetadataDtoMapper;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.schemaorg.SchemaOrgDocument;
import no.unit.nva.transformer.Transformer;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class FetchPublicationHandler extends ApiGatewayHandler<Void, String> {

    private static final String REGISTRATION_PATH = "registration";
    protected static final String ENV_NAME_NVA_FRONTEND_DOMAIN = "NVA_FRONTEND_DOMAIN";
    public static final Clock CLOCK = Clock.systemDefaultZone();
    private final ResourceService resourceService;

    private int statusCode = HttpURLConnection.HTTP_OK;

    @JacocoGenerated
    public FetchPublicationHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient());
    }

    @JacocoGenerated
    public FetchPublicationHandler(AmazonDynamoDB client) {
        this(defaultResourceService(client), new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     * @param environment     environment
     */
    public FetchPublicationHandler(ResourceService resourceService, Environment environment) {
        super(Void.class, environment);
        this.resourceService = resourceService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(
            HTML_UTF_8,
            ANY_TEXT_TYPE,
            XHTML_UTF_8,
            APPLICATION_JSON_LD,
            APPLICATION_DATACITE_XML,
            SCHEMA_ORG,
            JSON_UTF_8
        );
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        statusCode = HttpURLConnection.HTTP_OK; // make sure to reset to default on each invocation

        var identifier = RequestUtil.getIdentifier(requestInfo);
        var publication = resourceService.getPublicationByIdentifier(identifier);
        temporaryHackWhileWeWaitForUnauthenticatedUserAccess(publication);
        return createResponse(requestInfo, publication);
    }

    // TODO: implement unauthenticated users to allow us to remove unpublished files from returned data when the user
    //  is unauthenticated.
    private static void temporaryHackWhileWeWaitForUnauthenticatedUserAccess(Publication publication) {
        if (PublicationStatus.PUBLISHED_METADATA.equals(publication.getStatus())) {
            publication.setStatus(PublicationStatus.PUBLISHED);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return statusCode;
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService(AmazonDynamoDB client) {
        return new ResourceService(client, CLOCK);
    }

    private String createResponse(RequestInfo requestInfo,
                                  Publication publication)
        throws UnsupportedAcceptHeaderException {
        String response = null;
        var contentType = getDefaultResponseContentTypeHeaderValue(requestInfo);
        if (APPLICATION_DATACITE_XML.equals(contentType)) {
            response = createDataCiteMetadata(publication);
        } else if (SCHEMA_ORG.equals(contentType)) {
            response = createSchemaOrgRepresentation(publication);
        } else if (contentType.is(ANY_TEXT_TYPE) || XHTML_UTF_8.equals(contentType)) {
            statusCode = HTTP_SEE_OTHER;
            addAdditionalHeaders(() -> Map.of(LOCATION, landingPageLocation(publication.getIdentifier()).toString()));
        } else {
            response = createPublicationResponse(requestInfo, publication);
        }
        return response;
    }

    private URI landingPageLocation(SortableIdentifier identifier) {
        return new UriWrapper(HTTPS, environment.readEnv(ENV_NAME_NVA_FRONTEND_DOMAIN))
                   .addChild(REGISTRATION_PATH)
                   .addChild(identifier.toString())
                   .getUri();
    }

    private String createPublicationResponse(RequestInfo requestInfo, Publication publication) {
        var publicationResponse = PublicationMapper
                                      .convertValue(publication, PublicationResponse.class);
        return attempt(() -> getObjectMapper(requestInfo).writeValueAsString(publicationResponse)).orElseThrow();
    }

    private String createDataCiteMetadata(Publication publication) {
        var dataCiteMetadataDto = DataCiteMetadataDtoMapper.fromPublication(publication);
        return attempt(() -> new Transformer(dataCiteMetadataDto).asXml()).orElseThrow();
    }

    private String createSchemaOrgRepresentation(Publication publication) {
        return SchemaOrgDocument.fromPublication(publication);
    }
}
