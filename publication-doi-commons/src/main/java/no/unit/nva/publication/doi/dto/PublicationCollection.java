package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PublicationCollection {
    private final List<Publication> items;

    @JsonCreator
    public PublicationCollection(@JsonProperty("publications") List<Publication> publicationList) {
        this.items = publicationList;
    }

    public List<Publication> getItems() {
        return items;
    }
}
