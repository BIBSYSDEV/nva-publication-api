package no.sikt.nva.brage.migration.testutils;

import com.amazonaws.services.kms.model.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.SingletonCollector;

public class FakeResourceService extends ResourceService {

    public static SortableIdentifier SORTABLE_IDENTIFIER = ResourceService.DEFAULT_IDENTIFIER_SUPPLIER.get();
    private final List<Publication> publicationList;

    public FakeResourceService() {
        super(null, null, null);
        this.publicationList = new ArrayList<>();
    }

    public void addPublicationWithCristinIdentifier(Publication publication) {
        publicationList.add(publication);
    }

    @Override
    public Publication createPublicationFromImportedEntry(Publication publication) {
        var importedPublication = publishPublication(publication);
        publicationList.add(importedPublication);
        return importedPublication;
    }

    @Override
    public List<Publication> getPublicationsByCristinIdentifier(String cristinId) {
        return publicationList;
    }

    @Override
    public Publication getPublicationByIdentifier(SortableIdentifier identifier) {
        return publicationList.stream()
                   .filter(publication -> publication.getIdentifier().equals(identifier))
                   .collect(SingletonCollector.collect());
    }

    @Override
    public Publication updatePublication(Publication resourceUpdate) {
        var correspondingPublication =
            publicationList.stream()
                .filter(publication -> publication.getIdentifier().equals(resourceUpdate.getIdentifier()))
                .findAny();
        if (correspondingPublication.isPresent()) {
            publicationList.remove(correspondingPublication.get());
            publicationList.add(resourceUpdate);
            return resourceUpdate;
        } else {
            throw new NotFoundException("Not found");
        }
    }

    public List<Publication> getPublicationsThatHasBeenCreatedByImportedEntry() {
        return publicationList;
    }

    private Publication publishPublication(Publication publication) {
        return publication.copy().withIdentifier(SORTABLE_IDENTIFIER).withStatus(PublicationStatus.PUBLISHED).build();
    }
}
