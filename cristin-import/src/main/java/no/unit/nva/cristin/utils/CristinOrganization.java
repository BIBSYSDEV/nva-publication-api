package no.unit.nva.cristin.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record CristinOrganization(URI id, List<CristinOrganization> partOf) implements JsonSerializable {

    public static final int SINGLE_CRISTIN_ORGANIZATION = 0;

    @JsonIgnore
    public CristinOrganization getTopLevelOrganization() {
        return partOf().get(SINGLE_CRISTIN_ORGANIZATION);
    }

}