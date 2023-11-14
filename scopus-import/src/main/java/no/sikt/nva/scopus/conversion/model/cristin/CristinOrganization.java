package no.sikt.nva.scopus.conversion.model.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public class CristinOrganization implements JsonSerializable {

    @JsonProperty("id")
    private final URI id;
    @JsonProperty("name")
    private final Map<String, String> labels;
    @JsonProperty("country")
    private final String country;

    @JsonCreator
    public CristinOrganization(@JsonProperty("id") URI id,
                               @JsonProperty("name") Map<String, String> labels,
                               @JsonProperty("country") String country) {
        this.id = id;
        this.labels = labels;
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    @JsonCreator
    public URI getId() {
        return id;
    }

    @JsonCreator
    public Map<String, String> getLabels() {
        return labels;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getLabels());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CristinOrganization that)) {
            return false;
        }
        return Objects.equals(getId(), that.getId()) && Objects.equals(getLabels(), that.getLabels());
    }
}
