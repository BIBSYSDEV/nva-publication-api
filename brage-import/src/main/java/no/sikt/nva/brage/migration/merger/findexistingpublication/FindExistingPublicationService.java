package no.sikt.nva.brage.migration.merger.findexistingpublication;

import java.util.Optional;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;

public interface FindExistingPublicationService {


    Optional<PublicationForUpdate> findExistingPublication(PublicationRepresentation publicationRepresentation);

}
