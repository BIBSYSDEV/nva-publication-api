package no.unit.nva.publication.model.business.publicationchannel;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Locale;

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

    public static ChannelType fromChannelId(URI id) {
        if (id.getPath().toLowerCase(Locale.ROOT).contains("publisher")) {
            return PUBLISHER;
        } else {
            return SERIAL_PUBLICATION;
        }
    }

    @JsonProperty
    public String getValue() {
        return value;
    }
}
