package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.common.net.MediaType.ANY_TEXT_TYPE;
import static com.google.common.net.MediaType.HTML_UTF_8;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.net.MediaType.XHTML_UTF_8;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.RequestUtil.createUserInstanceFromRequest;
import static nva.commons.apigateway.MediaTypes.APPLICATION_DATACITE_XML;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.apigateway.MediaTypes.SCHEMA_ORG;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.doi.DataCiteMetadataDtoMapper;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.schemaorg.SchemaOrgDocument;
import no.unit.nva.transformer.Transformer;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.GoneException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class FetchPublicationHandler extends ApiGatewayHandler<Void, String> {

    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String GONE_MESSAGE = "Publication has been removed";
    protected static final String ENV_NAME_NVA_FRONTEND_DOMAIN = "NVA_FRONTEND_DOMAIN";
    private static final String REGISTRATION_PATH = "registration";
    public static final String DO_NOT_REDIRECT_QUERY_PARAM = "doNotRedirect";
    private final IdentityServiceClient identityServiceClient;
    private final ResourceService resourceService;
    private final RawContentRetriever authorizedBackendUriRetriever;
    private int statusCode = HttpURLConnection.HTTP_OK;

    @JacocoGenerated
    public FetchPublicationHandler() {
        this(ResourceService.defaultService(),
             new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME),
             new Environment(),
             IdentityServiceClient.prepare());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService publicationService
     * @param environment     environment
     */
    public FetchPublicationHandler(ResourceService resourceService,
                                   RawContentRetriever authorizedBackendUriRetriever,
                                   Environment environment,
                                   IdentityServiceClient identityServiceClient) {
        super(Void.class, environment);
        this.authorizedBackendUriRetriever = authorizedBackendUriRetriever;
        this.resourceService = resourceService;
        this.identityServiceClient = identityServiceClient;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(HTML_UTF_8, ANY_TEXT_TYPE, XHTML_UTF_8, APPLICATION_JSON_LD, APPLICATION_DATACITE_XML,
                       SCHEMA_ORG, JSON_UTF_8);
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        statusCode = HttpURLConnection.HTTP_OK; // make sure to reset to default on each invocation

        var identifier = RequestUtil.getIdentifier(requestInfo);
        var publication = resourceService.getPublicationByIdentifier(identifier);

        return switch (publication.getStatus()) {
            case DRAFT -> produceDraftPublicationResponse(requestInfo, publication);
            case PUBLISHED -> producePublishedPublicationResponse(requestInfo, publication);
            case UNPUBLISHED, DELETED -> produceRemovedPublicationResponse(publication, requestInfo);
            default -> throwNotFoundException();
        };
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return statusCode;
    }

    private String produceRemovedPublicationResponse(Publication publication, RequestInfo requestInfo)
        throws GoneException {
        if (nonNull(publication.getDuplicateOf()) && shouldRedirect(requestInfo)) {
            return produceRedirect(publication.getDuplicateOf());
        } else {
            var allowedOperations = getAllowedOperations(requestInfo, publication);
            var tombstone = DeletedPublicationResponse.fromPublication(publication, allowedOperations);
            throw new GoneException(GONE_MESSAGE, tombstone);
        }
    }

    private boolean shouldRedirect(RequestInfo requestInfo) {
        return attempt(() -> requestInfo.getQueryParameter(DO_NOT_REDIRECT_QUERY_PARAM))
                   .map(FetchPublicationHandler::shouldRedirect)
                   .orElse(fail -> true);
    }

    private static boolean shouldRedirect(String value) {
        return !Boolean.parseBoolean(value);
    }

    private String produceRedirect(URI duplicateOf) {
        statusCode = HTTP_MOVED_PERM;
        // cache control header here to avoid permanent browser caching of the redirect
        addAdditionalHeaders(() -> Map.of(LOCATION, duplicateOf.toString(), CACHE_CONTROL, "no-cache"));
        return null;
    }

    private String produceDraftPublicationResponse(RequestInfo requestInfo, Publication publication)
        throws UnsupportedAcceptHeaderException {
        return producePublishedPublicationResponse(requestInfo, publication);
    }

    @JacocoGenerated
    private String throwNotFoundException() throws NotFoundException {
        throw new NotFoundException("Publication is not found");
    }

    private String producePublishedPublicationResponse(RequestInfo requestInfo, Publication publication)
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
        return new UriWrapper(HTTPS, environment.readEnv(ENV_NAME_NVA_FRONTEND_DOMAIN)).addChild(REGISTRATION_PATH)
                   .addChild(identifier.toString())
                   .getUri();
    }

    private String createPublicationResponse(RequestInfo requestInfo, Publication publication) {
        //TODO: when the userIsCuratorOrOwner is properlyImplementedAgain,
        // then only those should get the PublicationResponseElevatedUser
        //Regular users should receive PublicationResponse.class
        var publicationResponse = PublicationMapper.convertValue(publication, PublicationResponseElevatedUser.class);
        publicationResponse.setAllowedOperations(getAllowedOperations(requestInfo, publication));
        return attempt(() -> getObjectMapper(requestInfo).writeValueAsString(publicationResponse)).orElseThrow();
    }

    private Set<PublicationOperation> getAllowedOperations(RequestInfo requestInfo, Publication publication) {
        return attempt(() -> createUserInstanceFromRequest(requestInfo, identityServiceClient)).toOptional()
                   .map(userInstance -> PublicationPermissionStrategy.create(publication, userInstance, resourceService))
                   .map(PublicationPermissionStrategy::getAllAllowedActions)
                   .orElse(Collections.emptySet());
    }

    private String createDataCiteMetadata(Publication publication) {
        var dataCiteMetadataDto = DataCiteMetadataDtoMapper.fromPublication(publication, authorizedBackendUriRetriever);
        return attempt(() -> new Transformer(dataCiteMetadataDto).asXml()).orElseThrow();
    }

    private String createSchemaOrgRepresentation(Publication publication) {
        return SchemaOrgDocument.fromPublication(publication);
    }
}
