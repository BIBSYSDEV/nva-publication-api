package no.unit.nva.publication;

import no.unit.nva.model.PublicationSummary;

import java.util.List;

public class PublicationsByOwnerResponse {

    private List<PublicationSummary> publications;

    public PublicationsByOwnerResponse(List<PublicationSummary> publications) {
        this.publications = publications;
    }

    public List<PublicationSummary> getPublications() {
        return publications;
    }

    public void setPublications(List<PublicationSummary> publications) {
        this.publications = publications;
    }
}
