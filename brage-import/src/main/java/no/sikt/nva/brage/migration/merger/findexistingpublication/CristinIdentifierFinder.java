package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static java.util.Objects.nonNull;
import java.util.Optional;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.publication.service.impl.ResourceService;

public class CristinIdentifierFinder implements FindExistingPublicationService {


    private final ResourceService resourceService;
    private final DuplicatePublicationReporter duplicatePublicationReporter;

    public CristinIdentifierFinder(ResourceService resourceService,
                                   DuplicatePublicationReporter duplicatePublicationReporter) {
        this.resourceService = resourceService;
        this.duplicatePublicationReporter = duplicatePublicationReporter;
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        var cristinIdentifier = publicationRepresentation.brageRecord().getCristinId();
        if (nonNull(cristinIdentifier)) {
            var publications = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier).stream()
                                   .filter(item -> PublicationComparator
                                                       .publicationsMatch(item,
                                                                          publicationRepresentation.publication()))
                                   .toList();
            if (FindExistingPublicationService.moreThanOneDuplicateFound(publications)) {
                duplicatePublicationReporter.reportDuplicatePublications(publications,
                                                                         publicationRepresentation.brageRecord(),
                                                                         DuplicateDetectionCause.CRISTIN_DUPLICATES);
            }
            return publications.isEmpty()
                       ? Optional.empty()
                       : Optional.of(new PublicationForUpdate(MergeSource.CRISTIN, publications.getFirst()));
        }
        return Optional.empty();
    }
}
