package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("Doi")
public class DoiResponse {

    private final URI doi;

    @JsonCreator
    public DoiResponse(@JsonProperty("doi") URI doi) {
        this.doi = doi;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DoiResponse that = (DoiResponse) o;
        return Objects.equals(getDoi(), that.getDoi());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getDoi());
    }

    public URI getDoi() {
        return doi;
    }
}
