package no.unit.nva.publication.service.impl;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.ResourceSearchResult;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class SearchService {

  public static final String SEARCH = "search";
  public static final String RESOURCES = "resources";
  private static final String API_HOST = new Environment().readEnv("API_HOST");
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String SIZE = "size";
  private static final String SORT = "sort";
  private static final String AGGREGATION = "aggregation";
  private static final String SORT_BY_IDENTIFIER = "identifier";
  private static final String NO_AGGREGATION = "none";
  private static final Set<String> PAGINATION_PARAMETERS =
      Set.of(
          "from",
          SIZE,
          "page",
          "offset",
          SORT,
          "sortOrder",
          "order",
          "orderBy",
          "searchAfter",
          "search_after",
          AGGREGATION);
  private final UriRetriever uriRetriever;
  private final ResourceService resourceService;

  private SearchService(UriRetriever uriRetriever, ResourceService resourceService) {
    this.uriRetriever = uriRetriever;
    this.resourceService = resourceService;
  }

  public static SearchService create(UriRetriever uriRetriever, ResourceService resourceService) {
    return new SearchService(uriRetriever, resourceService);
  }

  public List<Resource> searchPublicationsByParam(Map<String, String> searchParams) {
    return searchResourcesByParam(searchParams).resources();
  }

  public ResourceSearchResult searchResourcesByParam(Map<String, String> searchParams) {
    return searchPage(searchUriFromSearchParams(searchParams));
  }

  public URI firstPageUri(Map<String, String> searchParams, int pageSize) {
    return searchUriFromSearchParams(paginatedSearchParams(searchParams, pageSize));
  }

  public ResourceSearchResult searchPage(URI pageUri) {
    var response = uriRetriever.fetchResponse(pageUri, CONTENT_TYPE_JSON).orElseThrow();
    if (response.statusCode() != HTTP_OK) {
      throw new SearchServiceException(response);
    }
    var searchResponse = toSearchResponse(response);
    return new ResourceSearchResult(
        searchResponse.totalHits(),
        searchResponse.hits().size(),
        fetchResources(searchResponse),
        searchResponse.nextSearchAfterResults());
  }

  private static Map<String, String> paginatedSearchParams(
      Map<String, String> searchParams, int pageSize) {
    var paginatedParams = new LinkedHashMap<>(searchParams);
    PAGINATION_PARAMETERS.forEach(paginatedParams::remove);
    paginatedParams.put(SIZE, String.valueOf(pageSize));
    paginatedParams.put(SORT, SORT_BY_IDENTIFIER);
    paginatedParams.put(AGGREGATION, NO_AGGREGATION);
    return paginatedParams;
  }

  private static URI searchUriFromSearchParams(Map<String, String> searchParams) {
    return UriWrapper.fromHost(API_HOST)
        .addChild(SEARCH)
        .addChild(RESOURCES)
        .addQueryParameters(searchParams)
        .getUri();
  }

  private List<Resource> fetchResources(SearchResourceApiResponse searchResponse) {
    return searchResponse.hits().stream()
        .map(ResourceWithId::getIdentifier)
        .map(this::fetchPublication)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private Optional<Resource> fetchPublication(SortableIdentifier identifier) {
    return Resource.resourceQueryObject(identifier).fetch(resourceService);
  }

  private SearchResourceApiResponse toSearchResponse(HttpResponse<String> response) {
    return attempt(response::body).map(SearchResourceApiResponse::fromBody).orElseThrow();
  }

  public static class SearchServiceException extends RuntimeException {

    public static final String EXCEPTION_MESSAGE = "Could not fetch resources: {0}";

    public SearchServiceException(HttpResponse<String> response) {
      super(MessageFormat.format(EXCEPTION_MESSAGE, response));
    }
  }
}
