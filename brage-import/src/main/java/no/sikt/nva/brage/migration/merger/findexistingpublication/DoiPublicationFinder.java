package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static java.util.Objects.nonNull;
import java.util.Optional;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;

public class DoiPublicationFinder implements FindExistingPublicationService {

    private static final String DOI = "doi";

    private final SearchApiFinder searchApiFinder;

    public DoiPublicationFinder(ResourceService resourceService,
                                UriRetriever uriRetriever,
                                String apiHost) {
        this.searchApiFinder = new SearchApiFinder(resourceService, uriRetriever, apiHost);
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        if (hasDoi(publicationRepresentation.publication())) {
            return existingPublicationHasSameDoi(publicationRepresentation.publication());
        }
        return Optional.empty();
    }

    private static boolean hasDoi(Publication publication) {
        return nonNull(publication.getEntityDescription().getReference().getDoi());
    }

    private Optional<PublicationForUpdate> existingPublicationHasSameDoi(Publication publication) {
        var doi = publication.getEntityDescription().getReference().getDoi();

        var publicationsByDoi = searchApiFinder.fetchPublicationsByParam(DOI, doi.toString()).stream()
                                    .filter(item -> PublicationComparator.publicationsMatch(item, publication))
                                    .toList();
        if (publicationsByDoi.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PublicationForUpdate(MergeSource.DOI, publicationsByDoi.getFirst()));
    }
}
