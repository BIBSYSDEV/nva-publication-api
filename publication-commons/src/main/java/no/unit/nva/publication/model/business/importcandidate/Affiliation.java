package no.unit.nva.publication.model.business.importcandidate;

import java.util.List;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public record Affiliation(String scopusAffiliationId,
                          String scopusDepartmentId,
                          List<String> names,
                          String text,
                          Country country,
                          Address address) implements ScopusOrganization {

    public static Affiliation emptyAffiliation() {
        return new Affiliation(null, null, null, null, null, null);
    }
}
