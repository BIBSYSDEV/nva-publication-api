package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TitleAndTypePublicationFinder implements FindExistingPublicationService {

    private static final String TITLE_SHOULD = "titleShould";
    private static final String CONTEXT_TYPE = "contextType";
    private static final Logger logger = LoggerFactory.getLogger(TitleAndTypePublicationFinder.class);
    private static final String RESOURCES = "resources";
    private static final String SEARCH = "search";
    private static final String EVENT = "Event";
    private static final String CONTENT_TYPE_JSON = "application/json";
    protected static final String UNCONFIRMED_JOURNAL = "UnconfirmedJournal";
    protected static final String JOURNAL = "Journal";
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
        if (getMainTitle(publicationRepresentation.publication()).isEmpty()
            || getInstanceType(publicationRepresentation.publication()).isEmpty()) {
            return Optional.empty();
        }
        var potentialExistingPublications = searchForPublicationsByTypeAndTitle(
            publicationRepresentation.publication());
        if (potentialExistingPublications.isEmpty()) {
            return Optional.empty();
        }
        if (FindExistingPublicationService.moreThanOneDuplicateFound(potentialExistingPublications)) {
            duplicatePublicationReporter.reportDuplicatePublications(potentialExistingPublications,
                                                                            publicationRepresentation.brageRecord(),
                                                                     DuplicateDetectionCause.TITLE_DUPLICATES);
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
                   .map(SearchResourceApiResponse::hits)
                   .orElse(List.of())
                   .stream()
                   .map(ResourceWithId::getIdentifier)
                   .map(this::getPublicationByIdentifier)
                   .flatMap(Optional::stream)
                   .filter(item -> PublicationComparator.publicationsMatch(item, publication))
                   .toList();
    }

    private Optional<Publication> getPublicationByIdentifier(SortableIdentifier identifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifier)).toOptional();
    }

    private Optional<String> fetchResponse(URI uri) {
        var response = uriRetriever.fetchResponse(uri, CONTENT_TYPE_JSON).orElseThrow();
        if (isNotHttpOk(response)) {
            logger.info("Search-api responded with statusCode: {} for request: {}", response.statusCode(), uri);
            return Optional.empty();
        } else {
            return Optional.ofNullable(response.body());
        }
    }

    private URI searchByTypeAndTitleUri(Publication publication) {
        var additionalQueryParam = getAdditionalQueryParam(publication);
        var searchUri = getStandardSearchUri(publication);
        return additionalQueryParam.isPresent()
                   ? searchUri.addQueryParameter(additionalQueryParam.get().name(),
                                                 additionalQueryParam.get().value()).getUri()
                   : searchUri.getUri();
    }

    private UriWrapper getStandardSearchUri(Publication publication) {
        var mainTitle = getMainTitle(publication).orElseThrow();
        return UriWrapper.fromHost(apiHost)
                   .addChild(SEARCH)
                   .addChild(RESOURCES)
                   .addQueryParameter(TITLE_SHOULD, mainTitle)
                   .addQueryParameter(CONTEXT_TYPE, getPublicationContextType(publication));
    }

    private String getPublicationContextType(Publication publication) {
        return publication.getEntityDescription().getReference().getPublicationContext().getClass().getSimpleName();
    }

    private Optional<QueryParam> getAdditionalQueryParam(Publication publication) {
        var context = publication.getEntityDescription().getReference().getPublicationContext();
        return switch (context) {
            case ConferenceReport unused -> Optional.of(new QueryParam(CONTEXT_TYPE, EVENT));
            case Journal unused -> Optional.of(new QueryParam(CONTEXT_TYPE, UNCONFIRMED_JOURNAL));
            case UnconfirmedJournal unused -> Optional.of(new QueryParam(CONTEXT_TYPE, JOURNAL));
            default -> Optional.empty();
        };
    }

    private SearchResourceApiResponse toResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, SearchResourceApiResponse.class))
                   .orElseThrow();
    }
}
