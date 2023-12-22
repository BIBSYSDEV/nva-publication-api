package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(FundingSource.TYPE)
public class FundingSource implements JsonSerializable {

    public static final String TYPE = "FundingSource";
    private final URI id;

    @JsonCreator
    public FundingSource(@JsonProperty("id") URI id) {
        this.id = id;
    }

    public static FundingSource withId(URI id) {
        return new FundingSource(id);
    }

    public URI getId() {
        return id;
    }
}
