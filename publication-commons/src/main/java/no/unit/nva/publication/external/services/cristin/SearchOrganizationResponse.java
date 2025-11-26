package no.unit.nva.publication.external.services.cristin;

import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public record SearchOrganizationResponse(List<CristinOrganization> hits, int size) implements JsonSerializable {

    @Override
    public String toString() {
        return this.toJsonString();
    }
}
