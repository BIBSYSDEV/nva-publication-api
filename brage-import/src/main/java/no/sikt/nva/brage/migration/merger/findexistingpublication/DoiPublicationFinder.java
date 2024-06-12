package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;

public class DoiPublicationFinder implements FindExistingPublicationService {

    private static final String DOI = "doi";

    private final SearchApiFinder searchApiFinder;
    private final DuplicatePublicationReporter duplicatePublicationReporter;

    public DoiPublicationFinder(ResourceService resourceService,
                                UriRetriever uriRetriever,
                                String apiHost,
                                DuplicatePublicationReporter duplicatePublicationReporter) {
        this.searchApiFinder = new SearchApiFinder(resourceService, uriRetriever, apiHost);
        this.duplicatePublicationReporter = duplicatePublicationReporter;
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        if (hasDoi(publicationRepresentation.publication())) {
            return existingPublicationHasSameDoi(publicationRepresentation);
        }
        return Optional.empty();
    }

    private static boolean hasDoi(Publication publication) {
        return nonNull(publication.getEntityDescription().getReference().getDoi());
    }

    private Optional<PublicationForUpdate> existingPublicationHasSameDoi(PublicationRepresentation publicationRepresentation ) {
        var doi = publicationRepresentation.publication().getEntityDescription().getReference().getDoi();

        var publicationsByDoi = searchApiFinder.fetchPublicationsByParam(DOI, doi.toString()).stream()
                                    .filter(item -> PublicationComparator.publicationsMatch(item, publicationRepresentation.publication()))
                                    .toList();
        if (publicationsByDoi.isEmpty()) {
            return Optional.empty();
        }
        reportMultipleDuplicatesIfNecessary(publicationsByDoi, publicationRepresentation.brageRecord());

        return Optional.of(new PublicationForUpdate(MergeSource.DOI, publicationsByDoi.getFirst()));
    }

    private void reportMultipleDuplicatesIfNecessary(List<Publication> publicationsByDoi, Record record) {
        if (FindExistingPublicationService.moreThanOneDuplicateFound(publicationsByDoi)){
            duplicatePublicationReporter.reportDuplicatePublications(publicationsByDoi, record, DuplicateDetectionCause.DOI_DUPLICATES);
        }
    }
}
