package no.unit.nva.publication.model.business.publicationchannel;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ChannelType {

    PUBLISHER("Publisher"), SERIAL_PUBLICATION("SerialPublication");

    private final String value;

    ChannelType(String value) {
        this.value = value;
    }

    public static ChannelType fromValue(String value) {
        return stream(values()).filter(publicationStatus -> publicationStatus.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow();
    }

    @JsonProperty
    public String getValue() {
        return value;
    }
}
