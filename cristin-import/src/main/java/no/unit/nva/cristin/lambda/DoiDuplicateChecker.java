package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.cristin.mapper.SearchResource2Response;
import no.unit.nva.cristin.mapper.nva.exceptions.DuplicateDoiException;
import nva.commons.core.paths.UriWrapper;

public class DoiDuplicateChecker {

    private static final String SEARCH = "search";
    private static final String RESOURCES2 = "resources2";
    private static final String DOI = "doi";
    private static final String APPLICATION_JSON = "application/json";
    private final UriRetriever uriRetriever;
    private final String apiHost;

    public DoiDuplicateChecker(UriRetriever uriRetriever, String apiHost) {
        this.uriRetriever = uriRetriever;
        this.apiHost = apiHost;
    }

    public PublicationRepresentations throwIfDoiExists(PublicationRepresentations pubRep) {
        return Optional.ofNullable(pubRep.getIncomingPublication().getEntityDescription().getReference().getDoi())
                   .map(doi -> fetchNvaPublicationsByDoi(doi, pubRep))
                   .orElse(pubRep);
    }

    private PublicationRepresentations fetchNvaPublicationsByDoi(URI doi, PublicationRepresentations pubRep) {
        var request = constructSearchUri(doi);
        return getResponseBody(request)
                   .map(response -> checkForHits(response, doi, pubRep))
                   .orElse(pubRep);
    }

    private URI constructSearchUri(URI doi) {
        return UriWrapper.fromHost(apiHost)
                   .addChild(SEARCH)
                   .addChild(RESOURCES2)
                   .addQueryParameter(DOI, doi.toString())
                   .getUri();
    }

    private Optional<String> getResponseBody(URI uri) {
        return uriRetriever.getRawContent(uri, APPLICATION_JSON);
    }

    private PublicationRepresentations checkForHits(String response, URI doi, PublicationRepresentations pubRep) {
        var searchResponse = toResponse(response);
        if (responseHasHits(searchResponse)) {
            throw new DuplicateDoiException(doi);
        }
        return pubRep;
    }

    private SearchResource2Response toResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, SearchResource2Response.class))
                   .orElseThrow();
    }

    private static boolean responseHasHits(SearchResource2Response searchResponse) {
        return searchResponse.totalHits() > 0;
    }
}
