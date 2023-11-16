package no.sikt.nva.scopus.conversion.model.cristin;

import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public record SearchOrganizationResponse(List<CristinOrganization> hits, int size) implements JsonSerializable {

    public String toString() {
        return this.toJsonString();
    }

}
