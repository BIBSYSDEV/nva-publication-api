package no.unit.nva.publication.doi.update.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

public class PublicationHolder {

    public static final String TOPIC = "topic";
    public static final String ITEM = "item";

    @JsonProperty(TOPIC)
    private final String topic;
    @JsonProperty(ITEM)
    private final Publication item;

    @JacocoGenerated
    @JsonCreator
    public PublicationHolder(
        @JsonProperty(TOPIC) String type,
        @JsonProperty(ITEM) Publication publication) {
        this.topic = type;
        this.item = publication;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getItem());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicationHolder)) {
            return false;
        }
        PublicationHolder that = (PublicationHolder) o;
        return Objects.equals(getTopic(), that.getTopic()) && Objects.equals(getItem(), that.getItem());
    }

    @JacocoGenerated
    public String getTopic() {
        return topic;
    }

    @JacocoGenerated
    public Publication getItem() {
        return item;
    }
}
