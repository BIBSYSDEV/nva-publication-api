package no.unit.nva.publication.fetch;

import java.util.List;
import no.unit.nva.publication.model.PublicationSummary;

public class PublicationsByOwnerResponse {
    
    private final List<PublicationSummary> publications;
    
    public PublicationsByOwnerResponse(List<PublicationSummary> publications) {
        this.publications = publications;
    }
    
    public List<PublicationSummary> getPublications() {
        return publications;
    }
}
