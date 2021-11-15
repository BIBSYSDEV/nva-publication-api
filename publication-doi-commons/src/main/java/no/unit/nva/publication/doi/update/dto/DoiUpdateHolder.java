package no.unit.nva.publication.doi.update.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class DoiUpdateHolder implements JsonSerializable {

    public static final String TOPIC = "topic";
    public static final String ITEM = "item";

    @JsonProperty(TOPIC)
    protected String topic;
    @JsonProperty(ITEM)
    protected DoiUpdateDto item;

    @JsonCreator
    public DoiUpdateHolder(@JsonProperty(TOPIC) String topic, @JsonProperty(ITEM) DoiUpdateDto item) {
        this.topic = topic;
        this.item = item;
    }

    @JacocoGenerated
    public String getTopic() {
        return topic;
    }

    @JsonIgnore
    public boolean hasItem() {
        return item != null;
    }

    @JacocoGenerated
    public DoiUpdateDto getItem() {
        return item;
    }
}
