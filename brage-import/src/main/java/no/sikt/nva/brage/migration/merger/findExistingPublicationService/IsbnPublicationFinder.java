package no.sikt.nva.brage.migration.merger.findExistingPublicationService;

import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.merger.DuplicatePublicationException;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;

public class IsbnPublicationFinder {

    public static final String ISBN = "isbn";

    private final SearchApiFinder searchApiFinder;

    public IsbnPublicationFinder(ResourceService resourceService, UriRetriever uriRetriever, String apiHost) {

        this.searchApiFinder = new SearchApiFinder(resourceService, uriRetriever, apiHost);
    }

    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        if (hasIsbn(publicationRepresentation.publication())) {
            return existingPublicationHasSameIsbn(publicationRepresentation.publication());
        }
        return Optional.empty();
    }

    private boolean hasIsbn(Publication publication) {
        if (publication.getEntityDescription().getReference().getPublicationContext() instanceof Book book) {
            return !book.getIsbnList().isEmpty();
        } else {
            return false;
        }
    }

    private Optional<PublicationForUpdate> existingPublicationHasSameIsbn(Publication publication) {
        var isbnList = ((Book) publication.getEntityDescription().getReference().getPublicationContext()).getIsbnList();
        var publicationsToMerge = isbnList.stream()
                                      .map(isbn -> searchApiFinder.fetchPublicationsByParam(ISBN, isbn))
                                      .flatMap(List::stream)
                                      .filter(item -> PublicationComparator.publicationsMatch(item, publication))
                                      .toList();
        if (publicationsToMerge.size() > 1) {
            throw new DuplicatePublicationException("More than one ISBN found");
        }
        if (publicationsToMerge.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PublicationForUpdate(MergeSource.ISBN, publicationsToMerge.getFirst()));
    }
}
