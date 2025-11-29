package no.sikt.nva.iri;

import com.fasterxml.jackson.annotation.JsonValue;

import java.net.URI;
import java.time.Year;
import java.util.UUID;

public record SerialPublicationId(UUID identifier, Year year) implements PublicationChannelId {

    private static final ChannelType CHANNEL_TYPE = ChannelType.SERIAL_PUBLICATION;

    public SerialPublicationId(UuidYearPair uuidYearPair) {
        this(uuidYearPair.uuid(), uuidYearPair.year());
    }

    public static SerialPublicationId from(URI uri) {
        return new SerialPublicationId(PublicationChannelId.validate(uri, CHANNEL_TYPE));
    }

    @Override
    public ChannelType type() {
        return CHANNEL_TYPE;
    }

    @JsonValue
    @Override
    public String value() {
        return PublicationChannelId.value(CHANNEL_TYPE, identifier, year);
    }
}
