package no.sikt.nva.brage.migration.testutils;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.impl.ResourceService;

public class FakeResourceService extends ResourceService {

    private final List<Publication> publicationList;
    public static SortableIdentifier SORTABLE_IDENTIFIER = ResourceService.DEFAULT_IDENTIFIER_SUPPLIER.get();

    public FakeResourceService() {
        super(null, null, null);
        this.publicationList = new ArrayList<>();
    }

    @Override
    public Publication createPublicationFromImportedEntry(Publication publication) {
        publication.setIdentifier(SORTABLE_IDENTIFIER);
        publication.setStatus(PublicationStatus.PUBLISHED);
        publicationList.add(publication);
        return publication;
    }

    public List<Publication> getPublicationsThatHasBeenCreatedByImportedEntry() {
        return publicationList;
    }
}
