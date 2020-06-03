package no.unit.nva.publication.query;

import java.util.List;
import no.unit.nva.publication.model.PublicationSummary;

public class PublishedPublicationsResponse {

    private final List<PublicationSummary> publications;

    public PublishedPublicationsResponse(List<PublicationSummary> publications) {
        this.publications = publications;
    }

    public List<PublicationSummary> getPublications() {
        return publications;
    }
}
