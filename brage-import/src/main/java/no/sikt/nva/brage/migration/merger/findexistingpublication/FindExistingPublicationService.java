package no.sikt.nva.brage.migration.merger.findexistingpublication;

import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.Publication;

public interface FindExistingPublicationService {

    int DUPLICATE_PUBLICATIONS_COUNT = 2;


    Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation);

    static boolean moreThanOneDuplicateFound(List<Publication> publications) {
        return publications.size() >= DUPLICATE_PUBLICATIONS_COUNT;
    }

}
