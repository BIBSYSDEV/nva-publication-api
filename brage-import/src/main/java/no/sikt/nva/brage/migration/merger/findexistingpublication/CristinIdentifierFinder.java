package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static java.util.Objects.nonNull;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.lambda.MergeSource;
import no.sikt.nva.brage.migration.lambda.PublicationComparator;
import no.sikt.nva.brage.migration.merger.DuplicatePublicationException;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;

public class CristinIdentifierFinder implements FindExistingPublicationService {
    public static final String SOURCE_CRISTIN = "Cristin";
    public static final String DUPLICATE_PUBLICATIONS_MESSAGE =
        "More than one publication with this cristin identifier already exists";

    private final ResourceService resourceService;

    public CristinIdentifierFinder(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation) {
        var cristinIdentifier = getCristinIdentifier(publicationRepresentation.publication());
        if (nonNull(cristinIdentifier)) {
            var publications = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier).stream()
                                   .filter(item -> PublicationComparator.publicationsMatch(item,
                                                                                           publicationRepresentation.publication()))
                                   .toList();
            if (publications.size() > 1) {
                throw new DuplicatePublicationException(DUPLICATE_PUBLICATIONS_MESSAGE);
            }
            return !publications.isEmpty()
                       ? Optional.of(new PublicationForUpdate(MergeSource.CRISTIN, publications.getFirst()))
                       : Optional.empty();
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
                   .map(AdditionalIdentifier::getValue)
                   .collect(Collectors.toSet());
    }

    private boolean isCristinIdentifier(AdditionalIdentifier identifier) {
        return SOURCE_CRISTIN.equals(identifier.getSourceName());
    }
}