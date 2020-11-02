package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import nva.commons.utils.JacocoGenerated;

public class PublicationCollection {
    private final String type;
    private final List<Publication> items;

    @JacocoGenerated
    @JsonCreator
    public PublicationCollection(
            @JsonProperty("type") String type,
            @JsonProperty("publications") List<Publication> publicationList) {
        this.type = type;
        this.items = publicationList;
    }

    public String getType() {
        return type;
    }

    public List<Publication> getItems() {
        return items;
    }
}
