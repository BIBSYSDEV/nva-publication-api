package no.unit.nva.publication.events.handlers.delete;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.ImportCandidateDeleteEvent;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteImportCandidateEventConsumer
    extends EventHandler<ImportCandidateDeleteEvent, Void> {

    public static final String TABLE_NAME = new Environment().readEnv("TABLE_NAME");
    public static final String EVENTS_BUCKET = new Environment().readEnv("EVENTS_BUCKET");
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String SEARCH = "search";
    public static final String IMPORT_CANDIDATES = "import-candidates";
    public static final String QUERY = "query";
    public static final String CONTENT_TYPE = "application/json";
    public static final int UNIQUE_HIT_FROM_SEARCH_API = 0;
    public static final String COULD_NOT_FETCH_UNIQUE_IMPORT_CANDIDATE_MESSAGE = "Could not fetch unique import "
                                                                                 + "candidate";
    public static final String NO_IMPORT_CANDIDATE_FOUND = "No import candidate found with scopus identifier %s";
    private static final Logger logger = LoggerFactory.getLogger(DeleteImportCandidateEventConsumer.class);
    private final ResourceService resourceService;
    private final UriRetriever uriRetriever;

    @JacocoGenerated
    public DeleteImportCandidateEventConsumer() {
        this(ResourceService.defaultService(TABLE_NAME), new UriRetriever());
    }

    protected DeleteImportCandidateEventConsumer(ResourceService resourceService, UriRetriever uriRetriever) {
        super(ImportCandidateDeleteEvent.class);
        this.resourceService = resourceService;
        this.uriRetriever = uriRetriever;
    }

    @Override
    protected Void processInput(ImportCandidateDeleteEvent input, AwsEventBridgeEvent<ImportCandidateDeleteEvent> event,
                                Context context) {
        attempt(input::getScopusIdentifier)
            .map(this::fetchImportCandidate)
            .forEach(importCandidate -> {
                if (importCandidate != null) {
                    resourceService.deleteImportCandidate(importCandidate);
                }
            })
            .orElseThrow();
        return null;
    }

    private static ExpandedImportCandidate toExpandedImportCandidate(ImportCandidateSearchApiResponse response)
        throws BadGatewayException {
        if (containsSingleHit(response)) {
            return response.getHits().get(UNIQUE_HIT_FROM_SEARCH_API);
        }
        throw new BadGatewayException(COULD_NOT_FETCH_UNIQUE_IMPORT_CANDIDATE_MESSAGE);
    }

    private static boolean containsSingleHit(ImportCandidateSearchApiResponse response) {
        return response.getTotal() == 1;
    }

    private static ImportCandidateSearchApiResponse toSearchApiResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, ImportCandidateSearchApiResponse.class))
                   .orElseThrow();
    }

    private static ImportCandidate toImportCandidate(ExpandedImportCandidate expandedImportCandidate) {
        return new ImportCandidate.Builder()
                   .withIdentifier(extractIdentifier(expandedImportCandidate))
                   .build();
    }

    private static SortableIdentifier extractIdentifier(ExpandedImportCandidate expandedImportCandidate) {
        return new SortableIdentifier(UriWrapper.fromUri(expandedImportCandidate.getIdentifier()).getLastPathElement());
    }

    private ImportCandidate fetchImportCandidate(String scopusIdentifier) {
        var searchApiResponse = attempt(() -> constructUri(scopusIdentifier))
                                    .map(this::getResponseBody)
                                    .map(Optional::get)
                                    .map(DeleteImportCandidateEventConsumer::toSearchApiResponse);

        if (isEmpty(searchApiResponse)) {
            logger.info(String.format(NO_IMPORT_CANDIDATE_FOUND, scopusIdentifier));
            return null;
        } else {
            return searchApiResponse
                       .map(DeleteImportCandidateEventConsumer::toExpandedImportCandidate)
                       .map(DeleteImportCandidateEventConsumer::toImportCandidate)
                       .orElseThrow();
        }
    }

    private static boolean isEmpty(Try<ImportCandidateSearchApiResponse> searchApiResponse) {
        return searchApiResponse.get().getHits().isEmpty();
    }

    private Optional<String> getResponseBody(URI uri) {
        return uriRetriever.getRawContent(uri, CONTENT_TYPE);
    }

    private URI constructUri(String scopusIdentifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(SEARCH)
                   .addChild(IMPORT_CANDIDATES)
                   .addQueryParameter(QUERY, constructQuery(scopusIdentifier))
                   .getUri();
    }

    private String constructQuery(String scopusIdentifier) {
        return "(additionalIdentifiers.value:\""
               + scopusIdentifier
               + "\")+AND+(additionalIdentifiers.source:\"scopusIdentifier\")";
    }
}
