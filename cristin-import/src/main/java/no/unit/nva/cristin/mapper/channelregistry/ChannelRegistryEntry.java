package no.unit.nva.cristin.mapper.channelregistry;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import nva.commons.core.SingletonCollector;

public record ChannelRegistryEntry(String id, ChannelType type) {

    public static final String SERIAL_PUBLICATION_URI_PATH = "serial-publication";

    public static ChannelRegistryEntry fromChannelRegistryRepresentation(ChannelRegistryRepresentation representation) {
        return new ChannelRegistryEntry(representation.getPid(), ChannelType.fromValue(representation.getType()));
    }

    public String getEntryPath() {
        return SERIAL_PUBLICATION_URI_PATH;
    }

    public enum ChannelType {

        JOURNAL("Tidsskrift"), SERIES("Serie");
        private final String value;

        ChannelType(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        public static ChannelType fromValue(String value) {
            return Arrays.stream(ChannelType.values())
                       .filter(type -> type.getValue().equalsIgnoreCase(value))
                       .collect(SingletonCollector.collectOrElse(null));
        }
    }
}
