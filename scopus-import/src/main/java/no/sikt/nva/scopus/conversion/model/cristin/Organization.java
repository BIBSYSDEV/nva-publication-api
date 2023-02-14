package no.sikt.nva.scopus.conversion.model.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;

@SuppressWarnings("PMD.UnnecessaryAnnotationValueElement")
@JsonIgnoreProperties(value = {"type", "@context", "hasPart"})
public class Organization {

    @JsonProperty("id")
    private final URI id;
    @JsonProperty("name")
    private final Map<String, String> labels;

    @JsonCreator
    public Organization(@JsonProperty("id") URI id,
                        @JsonProperty("name") Map<String, String> labels) {
        this.id = id;
        this.labels = labels;
    }

    @JsonCreator
    public URI getId() {
        return id;
    }

    @JsonCreator
    public Map<String, String> getLabels() {
        return labels;
    }
}
