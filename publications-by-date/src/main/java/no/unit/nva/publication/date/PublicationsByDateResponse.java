package no.unit.nva.publication.date;

import java.util.List;
import no.unit.nva.publication.model.PublicationSummary;

public class PublicationsByDateResponse {

    private final List<PublicationSummary> publications;

    public PublicationsByDateResponse(List<PublicationSummary> publications) {
        this.publications = publications;
    }

    public List<PublicationSummary> getPublications() {
        return publications;
    }
}
