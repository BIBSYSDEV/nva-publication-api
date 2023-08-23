package no.unit.nva.expansion.model.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public class CristinOrganization implements JsonSerializable {

    public static final String PART_OF = "partOf";
    public static final String ID = "id";
    @JsonProperty(PART_OF)
    public final List<Organization> partOf;

    @JsonCreator
    public CristinOrganization(@JsonProperty(PART_OF) List<Organization> partOf) {
        this.partOf = partOf;
    }

    public List<Organization> getPartOf() {
        return partOf;
    }

    public static final class Organization {

        @JsonProperty(ID)
        public final URI id;

        @JsonCreator
        public Organization(@JsonProperty(ID) URI id) {
            this.id = id;
        }

        public URI getId() {
            return id;
        }
    }
}
