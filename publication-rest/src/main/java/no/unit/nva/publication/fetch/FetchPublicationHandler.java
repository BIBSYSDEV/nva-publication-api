package no.unit.nva.publication.fetch;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static nva.commons.apigateway.MediaTypes.APPLICATION_DATACITE_XML;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.apigateway.MediaTypes.SCHEMA_ORG;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.util.List;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.doi.DataCiteMetadataDtoMapper;
import no.unit.nva.model.Publication;
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

public class FetchPublicationHandler extends ApiGatewayHandler<Void, String> {
    
    public static final Clock CLOCK = Clock.systemDefaultZone();
    private final ResourceService resourceService;
    
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
            JSON_UTF_8,
            APPLICATION_JSON_LD,
            APPLICATION_DATACITE_XML,
            SCHEMA_ORG
        );
    }
    
    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        
        var identifier = RequestUtil.getIdentifier(requestInfo);
        var publication = resourceService.getPublicationByIdentifier(identifier);
        return createResponse(requestInfo, publication);
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }
    
    @JacocoGenerated
    private static ResourceService defaultResourceService(AmazonDynamoDB client) {
        return new ResourceService(client, CLOCK);
    }
    
    private String createResponse(RequestInfo requestInfo,
                                  Publication publication) throws UnsupportedAcceptHeaderException {
        String response;
        var contentType = getDefaultResponseContentTypeHeaderValue(requestInfo);
        if (APPLICATION_DATACITE_XML.equals(contentType)) {
            response = createDataCiteMetadata(publication);
        } else if (SCHEMA_ORG.equals(contentType)) {
            response = createSchemaOrgRepresentation(publication);
        } else {
            response = createPublicationResponse(requestInfo, publication);
        }
        return response;
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
