package no.sikt.nva.scopus.conversion;

import lombok.Getter;

@Getter
public enum PublicationChannel {

    SERIAL_PUBLICATION("serial-publication"),
    PUBLISHER("publisher");

    private final String value;

    PublicationChannel(String value) {
        this.value = value;
    }
}
