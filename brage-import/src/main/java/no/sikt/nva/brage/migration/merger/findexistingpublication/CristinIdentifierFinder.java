package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static java.util.Objects.nonNull;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.AdditionalIdentifierBase;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;

public class CristinIdentifierFinder implements FindExistingPublicationService {
    public static final String SOURCE_CRISTIN = "Cristin";


    private final ResourceService resourceService;
    private final DuplicatePublicationReporter duplicatePublicationReporter;

    public CristinIdentifierFinder(ResourceService resourceService, DuplicatePublicationReporter duplicatePublicationReporter) {
        this.resourceService = resourceService;
        this.duplicatePublicationReporter = duplicatePublicationReporter;
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        var cristinIdentifier = getCristinIdentifier(publicationRepresentation.publication());
        if (nonNull(cristinIdentifier)) {
            var publications = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier).stream()
                                   .filter(item -> PublicationComparator.publicationsMatch(item,
                                                                                           publicationRepresentation.publication()))
                                   .toList();
            if (FindExistingPublicationService.moreThanOneDuplicateFound(publications)) {
                duplicatePublicationReporter.reportDuplicatePublications(publications,
                                                                         publicationRepresentation.brageRecord(), DuplicateDetectionCause.CRISTIN_DUPLICATES);
            }
            return publications.isEmpty()
                       ? Optional.empty()
                       : Optional.of(new PublicationForUpdate(MergeSource.CRISTIN, publications.getFirst()));
        }
        return Optional.empty();
    }



    private String getCristinIdentifier(Publication publication) {
        var cristinIdentifiers = getCristinIdentifiers(publication);
        if (cristinIdentifiers.isEmpty()) {
            return null;
        }
        return cristinIdentifiers.iterator().next();
    }

    private Set<String> getCristinIdentifiers(Publication publication) {
        return publication.getAdditionalIdentifiers()
                   .stream()
                   .filter(this::isCristinIdentifier)
                   .map(AdditionalIdentifierBase::value)
                   .collect(Collectors.toSet());
    }

    private boolean isCristinIdentifier(AdditionalIdentifierBase identifier) {
        return SOURCE_CRISTIN.equals(identifier.sourceName());
    }
}
