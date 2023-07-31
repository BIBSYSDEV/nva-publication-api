package no.sikt.nva.scopus.conversion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import lombok.Getter;
import no.unit.nva.commons.json.JsonSerializable;

@Getter
public class PublicationChannelResponse implements JsonSerializable {

    public static final String ID = "id";
    @JsonProperty(ID)
    private final URI id;

    @JsonCreator
    public PublicationChannelResponse(@JsonProperty(ID) URI id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }
}
