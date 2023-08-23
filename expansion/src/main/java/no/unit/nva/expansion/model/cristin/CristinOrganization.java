package no.unit.nva.expansion.model.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;

public class CristinOrganization implements JsonSerializable {

    public static final String PART_OF = "partOf";
    public static final String ID = "id";
    @JsonProperty(ID)
    public final URI id;
    @JsonProperty(PART_OF)
    public final List<Organization> partOf;

    @JsonCreator
    public CristinOrganization(@JsonProperty(ID) URI id, @JsonProperty(PART_OF) List<Organization> partOf) {
        this.id = id;
        this.partOf = partOf;
    }

    public List<Organization> getPartOf() {
        return partOf;
    }

    public static final class Organization {

        public static final String LABELS = "labels";
        public static final String ID = "id";

        @JsonProperty(ID)
        public final URI id;
        @JsonProperty(LABELS)
        public final Map<String, String> labels;

        @JsonCreator
        public Organization(@JsonProperty(ID) URI id, @JsonProperty(LABELS) Map<String, String> labels) {
            this.id = id;
            this.labels = labels;
        }

        public URI getId() {
            return id;
        }
    }
}
