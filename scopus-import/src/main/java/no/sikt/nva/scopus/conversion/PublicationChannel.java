package no.sikt.nva.scopus.conversion;

import lombok.Getter;

@Getter
public enum PublicationChannel {

    JOURNAL("journal"),
    SERIES("series"),
    PUBLISHER("publisher");

    private final String value;

    PublicationChannel(String value) {
        this.value = value;
    }
}
