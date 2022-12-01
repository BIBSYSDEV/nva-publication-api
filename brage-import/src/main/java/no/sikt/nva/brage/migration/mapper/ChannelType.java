package no.sikt.nva.brage.migration.mapper;

public enum ChannelType {

    JOURNAL("journal"),
    PUBLISHER("publisher"),
    SERIES("series");

    private final String type;

    ChannelType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
