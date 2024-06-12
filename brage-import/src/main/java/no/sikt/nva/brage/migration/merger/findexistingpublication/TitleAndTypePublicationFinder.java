package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TitleAndTypePublicationFinder implements FindExistingPublicationService {

    public static final String TITLE = "title";
    public static final String CONTEXT_TYPE = "contextType";
    public static final String AGGREGATION = "aggregation";
    public static final String NONE = "none";
    private static final Logger logger = LoggerFactory.getLogger(TitleAndTypePublicationFinder.class);
    private static final String RESOURCES = "resources";
    private static final String SEARCH = "search";
    private final ResourceService resourceService;
    private final UriRetriever uriRetriever;
    private final String apiHost;
    private final DuplicatePublicationReporter duplicatePublicationReporter;

    public TitleAndTypePublicationFinder(ResourceService resourceService,
                                         UriRetriever uriRetriever,
                                         String apiHost,
                                         DuplicatePublicationReporter duplicatePublicationReporter) {
        this.resourceService = resourceService;
        this.uriRetriever = uriRetriever;
        this.apiHost = apiHost;
        this.duplicatePublicationReporter = duplicatePublicationReporter;
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        if (getMainTitle(publicationRepresentation.publication()).isEmpty() ||
            getInstanceType(publicationRepresentation.publication()).isEmpty()) {
            return Optional.empty();
        }
        var potentialExistingPublications = searchForPublicationsByTypeAndTitle(
            publicationRepresentation.publication());
        if (potentialExistingPublications.isEmpty()) {
            return Optional.empty();
        }
        if (FindExistingPublicationService.moreThanOneDuplicateFound(potentialExistingPublications)) {
            duplicatePublicationReporter.reportDuplicatePublications(potentialExistingPublications,
                                                                            publicationRepresentation.brageRecord(), DuplicateDetectionCause.TITLE_DUPLICATES);
        }
        return Optional.of(new PublicationForUpdate(MergeSource.SEARCH, potentialExistingPublications.getFirst()));
    }

    private static boolean isNotHttpOk(HttpResponse<String> response) {
        return response.statusCode() != HTTP_OK;
    }

    private static Optional<String> getInstanceType(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(PublicationInstance::getInstanceType);
    }

    private static Optional<String> getMainTitle(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getMainTitle);
    }

    private List<Publication> searchForPublicationsByTypeAndTitle(Publication publication) {
        var response = fetchResponse(searchByTypeAndTitleUri(publication));
        return response.map(this::toResponse)
                   .filter(SearchResourceApiResponse::containsSingleHit)
                   .map(SearchResourceApiResponse::hits)
                   .orElse(List.of())
                   .stream()
                   .map(ResourceWithId::getIdentifier)
                   .map(this::getPublicationByIdentifier)
                   .filter(item -> PublicationComparator.publicationsMatch(item, publication))
                   .collect(Collectors.toList()).reversed();
    }

    private Publication getPublicationByIdentifier(SortableIdentifier identifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifier)).orElseThrow();
    }

    private Optional<String> fetchResponse(URI uri) {
        var response = uriRetriever.fetchResponse(uri);
        if (isNotHttpOk(response)) {
            logger.info("Search-api responded with statusCode: {} for request: {}", response.statusCode(), uri);
            return Optional.empty();
        } else {
            return Optional.ofNullable(response.body());
        }
    }

    private URI searchByTypeAndTitleUri(Publication publication) {
        return UriWrapper.fromHost(apiHost)
                   .addChild(SEARCH)
                   .addChild(RESOURCES)
                   .addQueryParameter(TITLE, getMainTitle(publication).get())
                   .addQueryParameter(CONTEXT_TYPE, getInstanceType(publication).get())
                   .addQueryParameter(AGGREGATION, NONE)
                   .getUri();
    }

    private SearchResourceApiResponse toResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, SearchResourceApiResponse.class))
                   .orElseThrow();
    }
}
