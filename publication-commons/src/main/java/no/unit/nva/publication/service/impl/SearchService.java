package no.unit.nva.publication.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class SearchService {

    public static final String SIZE_VALUE = "100";
    public static final String SIZE_QUERY_PARAM = "size";
    public static final String SEARCH = "search";
    public static final String RESOURCES = "resources";
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private final UriRetriever uriRetriever;
    private final ResourceService resourceService;

    private SearchService(UriRetriever uriRetriever, ResourceService resourceService) {
        this.uriRetriever = uriRetriever;
        this.resourceService = resourceService;
    }

    public static SearchService create(UriRetriever uriRetriever, ResourceService resourceService) {
        return new SearchService(uriRetriever, resourceService);
    }

    public List<Publication> searchPublicationsByParam(Map<String, String> searchParams) {
        var uri = searchUriFromSearchParams(searchParams);
        var response = uriRetriever.fetchResponse(uri);
        return response.statusCode() == 200 ? processResponse(response) : throwException(response);
    }

    private static URI searchUriFromSearchParams(Map<String, String> searchParams) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(SEARCH)
                   .addChild(RESOURCES)
                   .addQueryParameters(searchParams)
                   .getUri();
    }

    private List<Publication> throwException(HttpResponse<String> response) {
        throw new SearchServiceException(response);
    }

    private List<Publication> processResponse(HttpResponse<String> response) {
        return getResourcesWithId(response).stream()
                   .map(ResourceWithId::getIdentifier)
                   .map(this::fetchPublication)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .toList();
    }

    private Optional<Publication> fetchPublication(SortableIdentifier identifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifier)).toOptional();
    }

    private List<ResourceWithId> getResourcesWithId(HttpResponse<String> response) {
        return attempt(response::body)
                   .map(SearchResourceApiResponse::fromBody)
                   .map(SearchResourceApiResponse::hits)
                   .orElseThrow();
    }

    public static class SearchServiceException extends RuntimeException {

        public static final String EXCEPTION_MESSAGE = "Could not fetch resources: {0}";

        public SearchServiceException(HttpResponse<String> response) {
            super(MessageFormat.format(EXCEPTION_MESSAGE, response));
        }
    }
}
