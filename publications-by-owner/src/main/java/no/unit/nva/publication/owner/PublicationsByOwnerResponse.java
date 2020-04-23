package no.unit.nva.publication.owner;

import no.unit.nva.publication.model.PublicationSummary;

import java.util.List;

public class PublicationsByOwnerResponse {

    private final List<PublicationSummary> publications;

    public PublicationsByOwnerResponse(List<PublicationSummary> publications) {
        this.publications = publications;
    }

    public List<PublicationSummary> getPublications() {
        return publications;
    }
}
