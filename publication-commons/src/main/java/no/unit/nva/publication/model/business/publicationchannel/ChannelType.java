package no.unit.nva.publication.model.business.publicationchannel;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import nva.commons.core.paths.UriWrapper;

public enum ChannelType {

    PUBLISHER("Publisher"), SERIAL_PUBLICATION("SerialPublication");

    private static final String PUBLISHER_PATH = "publisher";
    private static final String SERIAL_PUBLICATION_PATH = "serial-publication";
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
        var channelType = UriWrapper.fromUri(id).getPath().getPathElementByIndexFromEnd(2);
        return switch (channelType) {
            case PUBLISHER_PATH ->  PUBLISHER;
            case SERIAL_PUBLICATION_PATH -> SERIAL_PUBLICATION;
            default -> throw new IllegalArgumentException("Invalid channel type!");
        };
    }

    @JsonProperty
    public String getValue() {
        return value;
    }
}
