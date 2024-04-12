package no.sikt.nva.scopus.update;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import no.sikt.nva.scopus.ScopusHandler;
import no.sikt.nva.scopus.conversion.model.ImportCandidateSearchApiResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class ScopusUpdater {

    public static final String COULD_NOT_FETCH_UNIQUE_IMPORT_CANDIDATE_MESSAGE = "Could not fetch unique import "
                                                                                 + "candidate";
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private static final String SEARCH = "search";
    private static final String IMPORT_CANDIDATES_2 = "import-candidates2";
    private static final String SCOPUS_IDENTIFIER = "scopusIdentifier";
    private final ResourceService resourceService;
    private final UriRetriever uriRetriever;
    private static final String APPLICATION_JSON = "application/json";

    public ScopusUpdater(ResourceService resourceService, UriRetriever uriRetriever) {
        this.resourceService = resourceService;
        this.uriRetriever = uriRetriever;
    }

    public ImportCandidate updateImportCandidate(ImportCandidate importCandidate)
        throws NotFoundException {
        var existingImportCandidate = fetchImportCandidate(getScopusIdentifier(importCandidate));
        if (nonNull(existingImportCandidate)) {
            var persistedImportcandidate = resourceService.getImportCandidateByIdentifier(
                existingImportCandidate.getIdentifier());
            persistedImportcandidate.setEntityDescription(importCandidate.getEntityDescription());
            return persistedImportcandidate;
        }
        return importCandidate;
    }

    public ImportCandidateSearchApiResponse toSearchApiResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, ImportCandidateSearchApiResponse.class))
                   .orElseThrow();
    }

    public ImportCandidate toImportCandidate(ExpandedImportCandidate expandedImportCandidate) {
        return new ImportCandidate.Builder()
                   .withIdentifier(extractIdentifier(expandedImportCandidate))
                   .build();
    }

    public ExpandedImportCandidate toExpandedImportCandidate(ImportCandidateSearchApiResponse response)
        throws BadGatewayException {
        if (containsSingleHit(response)) {
            return response.getHits().getFirst();
        }
        throw new BadGatewayException(COULD_NOT_FETCH_UNIQUE_IMPORT_CANDIDATE_MESSAGE);
    }

    private static boolean containsSingleHit(ImportCandidateSearchApiResponse response) {
        return response.getTotal() == 1;
    }

    private static SortableIdentifier extractIdentifier(ExpandedImportCandidate expandedImportCandidate) {
        return new SortableIdentifier(UriWrapper.fromUri(expandedImportCandidate.getIdentifier()).getLastPathElement());
    }

    private URI constructUri(String scopusIdentifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(SEARCH)
                   .addChild(IMPORT_CANDIDATES_2)
                   .addQueryParameter(SCOPUS_IDENTIFIER, scopusIdentifier)
                   .getUri();
    }

    private ImportCandidate fetchImportCandidate(String scopusIdentifier) {
        return attempt(() -> constructUri(scopusIdentifier))
                   .map(this::getResponseBody)
                   .map(Optional::get)
                   .map(this::toSearchApiResponse)
                   .map(this::toExpandedImportCandidate)
                   .map(this::toImportCandidate)
                   .orElse(failure -> null);
    }

    private String getScopusIdentifier(ImportCandidate importCandidate) {
        return importCandidate.getAdditionalIdentifiers().stream()
                   .filter(this::isScopusIdentifier)
                   .map(AdditionalIdentifier::getValue)
                   .findFirst()
                   .orElse(null);
    }

    private Optional<String> getResponseBody(URI uri) {
        return uriRetriever.getRawContent(uri, APPLICATION_JSON);
    }

    private boolean isScopusIdentifier(AdditionalIdentifier identifier) {
        return ScopusHandler.SCOPUS_IDENTIFIER.equals(identifier.getSourceName());
    }
}
