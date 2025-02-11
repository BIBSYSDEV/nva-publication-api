package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.common.net.MediaType.ANY_TEXT_TYPE;
import static com.google.common.net.MediaType.HTML_UTF_8;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.net.MediaType.XHTML_UTF_8;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.service.impl.ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE;
import static nva.commons.apigateway.MediaTypes.APPLICATION_DATACITE_XML;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.apigateway.MediaTypes.SCHEMA_ORG;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.doi.DataCiteMetadataDtoMapper;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.PublicationResponseFactory;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
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
             IdentityServiceClient.prepare(),
             HttpClient.newHttpClient());
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
                                   IdentityServiceClient identityServiceClient,
                                   HttpClient httpClient) {
        super(Void.class, environment, httpClient);
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
        var resource = fetchResource(identifier);

        return switch (resource.getStatus()) {
            case DRAFT, PUBLISHED -> producePublicationResponse(requestInfo, resource);
            case UNPUBLISHED -> producePublicationResponseWhenUnpublished(requestInfo, resource);
            case DELETED -> produceRemovedPublicationResponse(resource, requestInfo);
            default -> throwNotFoundException();
        };
    }

    private String producePublicationResponseWhenUnpublished(RequestInfo requestInfo, Resource resource)
        throws GoneException {
        return userCanUpdateResource(requestInfo, resource)
                   ? createPublicationResponse(requestInfo, resource)
                   : produceRemovedPublicationResponse(resource, requestInfo);
    }

    private boolean userCanUpdateResource(RequestInfo requestInfo, Resource resource) {
        return getPublicationPermissionStrategy(requestInfo, resource)
                   .map(value -> value.allowsAction(UPDATE))
                   .orElse(false);
    }

    private Resource fetchResource(SortableIdentifier identifierInPath) throws NotFoundException {
        return Resource.resourceQueryObject(identifierInPath)
                   .fetch(resourceService)
                    .orElseThrow(
            () -> new NotFoundException(PUBLICATION_NOT_FOUND_CLIENT_MESSAGE + identifierInPath));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return statusCode;
    }

    private String produceRemovedPublicationResponse(Resource resource, RequestInfo requestInfo)
        throws GoneException {
        if (nonNull(resource.getDuplicateOf()) && shouldRedirect(requestInfo)) {
            return produceRedirect(resource.getDuplicateOf());
        } else {
            var publicationStrategy = getPublicationPermissionStrategy(requestInfo, resource);

            Set<PublicationOperation> allowedOperations =
                publicationStrategy.map(PublicationPermissions::getAllAllowedActions).orElse(emptySet());
            var tombstone = DeletedPublicationResponse.fromPublication(resource.toPublication(), allowedOperations);
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

    @JacocoGenerated
    private String throwNotFoundException() throws NotFoundException {
        throw new NotFoundException("Publication is not found");
    }

    private String producePublicationResponse(RequestInfo requestInfo, Resource resource)
        throws UnsupportedAcceptHeaderException {

        String response = null;
        var contentType = getDefaultResponseContentTypeHeaderValue(requestInfo);

        if (APPLICATION_DATACITE_XML.equals(contentType)) {
            response = createDataCiteMetadata(resource);
        } else if (SCHEMA_ORG.equals(contentType)) {
            response = createSchemaOrgRepresentation(resource);
        } else if (contentType.is(ANY_TEXT_TYPE) || XHTML_UTF_8.equals(contentType)) {
            statusCode = HTTP_SEE_OTHER;
            addAdditionalHeaders(() -> Map.of(LOCATION, landingPageLocation(resource.getIdentifier()).toString()));
        } else {
            response = createPublicationResponse(requestInfo, resource);
        }
        return response;
    }

    private URI landingPageLocation(SortableIdentifier identifier) {
        return new UriWrapper(HTTPS, environment.readEnv(ENV_NAME_NVA_FRONTEND_DOMAIN)).addChild(REGISTRATION_PATH)
                   .addChild(identifier.toString())
                   .getUri();
    }

    private String createPublicationResponse(RequestInfo requestInfo, Resource resource) {
        var response = PublicationResponseFactory.create(resource, requestInfo, identityServiceClient);
        return attempt(() -> getObjectMapper(requestInfo).writeValueAsString(response)).orElseThrow();
    }

    private Optional<PublicationPermissions> getPublicationPermissionStrategy(RequestInfo requestInfo,
                                                                              Resource resource) {
        return attempt(() -> RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient)).toOptional()
                   .map(userInstance -> PublicationPermissions.create(resource.toPublication(), userInstance));
    }

    private String createDataCiteMetadata(Resource resource) {
        var dataCiteMetadataDto = DataCiteMetadataDtoMapper.fromPublication(resource.toPublication(),
                                                                            authorizedBackendUriRetriever);
        return attempt(() -> new Transformer(dataCiteMetadataDto).asXml()).orElseThrow();
    }

    private String createSchemaOrgRepresentation(Resource resource) {
        return SchemaOrgDocument.fromPublication(resource.toPublication());
    }
}
