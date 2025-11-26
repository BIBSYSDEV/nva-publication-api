package no.unit.nva.publication.external.services.cristin;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

@JsonInclude(Include.NON_NULL)
public record CristinOrganization(@JsonProperty(ID) URI id,
                                  @JsonProperty(CONTEXT) URI context,
                                  @JsonProperty(TYPE) String type,
                                  @JsonProperty(PART_OF) List<CristinOrganization> partOf,
                                  @JsonProperty(COUNTRY) String country,
                                  @JsonProperty(LABELS) Map<String, String> labels)
    implements JsonSerializable {

    public static final String PART_OF = "partOf";
    public static final String ID = "id";
    public static final String LABELS = "labels";
    public static final String COUNTRY = "country";
    public static final String CONTEXT = "@context";
    private static final String TYPE = "type";

    @JacocoGenerated
    @JsonIgnore
    public CristinOrganization getTopLevelOrg() {
        if (hasPartOf(this)) {
            var organization = partOf().stream().collect(SingletonCollector.collect());
            while (hasPartOf(organization)) {
                organization = organization.partOf().stream().collect(SingletonCollector.collect());
            }
            return organization;
        }
        return this;
    }

    public Optional<URI> getTopLevelOrgId() {
        return Optional.ofNullable(getTopLevelOrg()).map(CristinOrganization::id);
    }

    public boolean containsLabelWithValue(String label) {
        return nonNull(labels) && labels.containsValue(label);
    }

    @JacocoGenerated
    private static boolean hasPartOf(CristinOrganization org) {
        return nonNull(org.partOf()) && !org.partOf().isEmpty();
    }
}
