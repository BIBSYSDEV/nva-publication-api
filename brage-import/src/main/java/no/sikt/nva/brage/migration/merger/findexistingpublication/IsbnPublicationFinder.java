package no.sikt.nva.brage.migration.merger.findexistingpublication;

import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.ResourceService;

public class IsbnPublicationFinder implements FindExistingPublicationService {

    public static final String ISBN = "isbn";

    private final SearchApiFinder searchApiFinder;
    private final DuplicatePublicationReporter duplicatePublicationReporter;

    public IsbnPublicationFinder(ResourceService resourceService, UriRetriever uriRetriever, String apiHost,
                                 DuplicatePublicationReporter duplicatePublicationReporter) {

        this.searchApiFinder = new SearchApiFinder(resourceService, uriRetriever, apiHost);
        this.duplicatePublicationReporter = duplicatePublicationReporter;
    }

    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        if (hasIsbn(publicationRepresentation.publication())) {
            return existingPublicationHasSameIsbn(publicationRepresentation);
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

    private Optional<PublicationForUpdate> existingPublicationHasSameIsbn(
        PublicationRepresentation publicationRepresentation) {
        var isbnList =
            ((Book) publicationRepresentation.publication().getEntityDescription()
                        .getReference().getPublicationContext()).getIsbnList();
        var publicationsToMerge = isbnList.stream()
                                      .map(isbn -> searchApiFinder.fetchPublicationsByParam(ISBN, isbn))
                                      .flatMap(List::stream)
                                      .filter(item ->
                                                  PublicationComparator.publicationsMatch(item,
                                                                                          publicationRepresentation
                                                                                              .publication()))
                                      .toList();
        if (FindExistingPublicationService.moreThanOneDuplicateFound(publicationsToMerge)) {
            duplicatePublicationReporter.reportDuplicatePublications(publicationsToMerge,
                                                                     publicationRepresentation.brageRecord(),
                                                                     DuplicateDetectionCause.ISBN_DUPLICATES);
        }
        if (publicationsToMerge.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PublicationForUpdate(MergeSource.ISBN, publicationsToMerge.getFirst()));
    }
}
