package no.sikt.nva.scopus.utils;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;

public class FakeResourceService extends ResourceService {

    private final List<Publication> publicationList;

    public FakeResourceService() {
        super(null, null, null);
        this.publicationList = new ArrayList<>();
    }

    @Override
    public List<Publication> getPublicationsByCristinIdentifier(String cristinId) {
        return publicationList;
    }
}
