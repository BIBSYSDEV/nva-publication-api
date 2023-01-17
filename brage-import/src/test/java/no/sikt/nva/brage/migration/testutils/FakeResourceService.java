package no.sikt.nva.brage.migration.testutils;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.DeletePublicationStatusResponse;
import no.unit.nva.publication.service.impl.ResourceService;

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
        publication.setIdentifier(SORTABLE_IDENTIFIER);
        publication.setStatus(PublicationStatus.PUBLISHED);
        publicationList.add(publication);
        return publication;
    }

    @Override
    public DeletePublicationStatusResponse updatePublishedStatusToDeleted(SortableIdentifier identifier) {
        var publication = publicationList.get(0).copy().withStatus(PublicationStatus.DELETED).build();
        publicationList.clear();
        publicationList.add(publication);
        return new DeletePublicationStatusResponse("nice", 200);
    }

    @Override
    public List<Publication> getPublicationsByCristinIdentifier(String cristinId) {
        return publicationList;
    }

    @Override
    public Publication getPublicationByIdentifier(SortableIdentifier identifier) {
        return publicationList.get(0);
    }

    @Override
    public Publication updatePublication(Publication resourceUpdate) {
        return Iterables.getOnlyElement(publicationList);
    }

    public List<Publication> getPublicationsThatHasBeenCreatedByImportedEntry() {
        return publicationList;
    }
}
