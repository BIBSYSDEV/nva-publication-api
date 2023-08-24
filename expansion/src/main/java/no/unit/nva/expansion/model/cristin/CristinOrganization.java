package no.unit.nva.expansion.model.cristin;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.SingletonCollector;

@Getter
public class CristinOrganization implements JsonSerializable {

    public static final String PART_OF = "partOf";
    public static final String ID = "id";
    public static final String LABELS = "labels";
    @JsonProperty(ID)
    private final URI id;
    @JsonProperty(PART_OF)
    private final List<CristinOrganization> partOf;
    @JsonProperty(LABELS)
    private final Map<String, String> labels;

    @JsonCreator
    public CristinOrganization(@JsonProperty(ID) URI id,
                               @JsonProperty(PART_OF) List<CristinOrganization> partOf,
                               @JsonProperty(LABELS) Map<String, String> labels) {
        this.id = id;
        this.partOf = partOf;
        this.labels = labels;
    }

    @JsonIgnore
    public CristinOrganization getTopLevelOrg() {
        if (nonNull(getPartOf())) {

            var organization = getPartOf().stream().collect(SingletonCollector.collect());

            while (hasPartOf(organization)) {
                organization = organization.getPartOf().stream().collect(SingletonCollector.collect());
            }

            return organization;
        }

        return this;
    }

    private static boolean hasPartOf(CristinOrganization org) {
        return nonNull(org.getPartOf()) && !org.getPartOf().isEmpty();
    }
}
