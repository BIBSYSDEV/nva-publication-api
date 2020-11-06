package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.utils.JacocoGenerated;

public class PublicationHolder {
    private final String type;
    private final Publication item;

    @JacocoGenerated
    @JsonCreator
    public PublicationHolder(
            @JsonProperty("type") String type,
            @JsonProperty("item") Publication publication) {
        this.type = type;
        this.item = publication;
    }

    public String getType() {
        return type;
    }

    public Publication getItem() {
        return item;
    }
}
