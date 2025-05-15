package no.unit.nva.publication.model.business.publicationchannel;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ChannelPolicy {

    OWNER_ONLY("OwnerOnly"), EVERYONE("Everyone");

    private final String value;

    ChannelPolicy(String value) {
        this.value = value;
    }

    public static ChannelPolicy fromValue(String value) {
        return stream(values()).filter(publicationStatus -> publicationStatus.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow();
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
