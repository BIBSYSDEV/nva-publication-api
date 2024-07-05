package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.paths.UriWrapper;

public class SearchApiFinder {

    private static final String APPLICATION_JSON = "application/json";
    private static final String SEARCH = "search";
    private static final String RESOURCES = "resources";
    public static final String AGGREGATION = "aggregation";
    public static final String NONE = "none";

    private final ResourceService resourceService;
    private final String apiHost;
    private final UriRetriever uriRetriever;

    protected SearchApiFinder(ResourceService resourceService, UriRetriever uriRetriever, String apiHost) {
        this.resourceService = resourceService;
        this.apiHost = apiHost;
        this.uriRetriever = uriRetriever;
    }

    protected List<Publication> fetchPublicationsByParam(String searchParam, String value) {
        var uri = searchPublicationByParamUri(searchParam, value);
        return uriRetriever.getRawContent(uri, APPLICATION_JSON)
                   .map(this::toResponse)
                   .map(SearchResourceApiResponse::hits)
                   .stream()
                   .flatMap(List::stream)
                   .map(ResourceWithId::getIdentifier)
                   .map(this::getPublicationByIdentifier)
                   .flatMap(Optional::stream)
                   .toList();
    }

    private URI searchPublicationByParamUri(String searchParam, String value) {
        return UriWrapper.fromHost(apiHost)
                   .addChild(SEARCH)
                   .addChild(RESOURCES)
                   .addQueryParameter(searchParam, value)
                   .addQueryParameter(AGGREGATION, NONE)
                   .getUri();
    }

    private SearchResourceApiResponse toResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, SearchResourceApiResponse.class))
                   .orElseThrow();
    }

    private Optional<Publication> getPublicationByIdentifier(SortableIdentifier identifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifier)).toOptional();
    }

}
