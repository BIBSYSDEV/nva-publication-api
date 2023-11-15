package no.unit.nva.expansion.model.cristin;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.SingletonCollector;

public record CristinOrganization(@JsonProperty(ID) URI id,
                                  @JsonProperty(CONTEXT) URI context,
                                  @JsonProperty(TYPE) String type,
                                  @JsonProperty(PART_OF) List<CristinOrganization> partOf,
                                  @JsonProperty(COUNTRY) String country,
                                  @JsonProperty(LABELS) Map<String, String> labels
)
    implements JsonSerializable {

    public static final String PART_OF = "partOf";
    public static final String ID = "id";
    public static final String LABELS = "labels";
    public static final String COUNTRY = "country";
    public static final String CONTEXT = "@context";
    private static final String TYPE = "type";

    @JsonCreator
    public CristinOrganization {
    }

    @JsonIgnore
    public CristinOrganization getTopLevelOrg() {
        if (nonNull(partOf())) {

            var organization = partOf().stream().collect(SingletonCollector.collect());

            while (hasPartOf(organization)) {
                organization = organization.partOf().stream().collect(SingletonCollector.collect());
            }

            return organization;
        }

        return this;
    }

    private static boolean hasPartOf(CristinOrganization org) {
        return nonNull(org.partOf()) && !org.partOf().isEmpty();
    }
}
