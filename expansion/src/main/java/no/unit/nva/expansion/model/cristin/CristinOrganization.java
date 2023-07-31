package no.unit.nva.expansion.model.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Organization;

@Getter
public class CristinOrganization implements JsonSerializable {

    public static final String PART_OF = "partOf";
    @JsonProperty(PART_OF)
    public final List<Organization> partOf;

    @JsonCreator
    public CristinOrganization(@JsonProperty(PART_OF) List<Organization> partOf) {
        this.partOf = partOf;
    }
}
