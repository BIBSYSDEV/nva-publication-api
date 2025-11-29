package no.sikt.nva.iri;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChannelType {
    SERIAL_PUBLICATION("serial-publication"),
    PUBLISHER("publisher");

    @JsonValue
    private final String type;

    ChannelType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
