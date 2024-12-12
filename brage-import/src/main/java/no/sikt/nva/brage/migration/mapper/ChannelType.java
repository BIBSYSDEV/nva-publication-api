package no.sikt.nva.brage.migration.mapper;

public enum ChannelType {

    SERIAL_PUBLICATION("serial-publication"),
    PUBLISHER("publisher");

    private final String type;

    ChannelType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
