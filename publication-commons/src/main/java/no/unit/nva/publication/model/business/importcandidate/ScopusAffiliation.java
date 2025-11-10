package no.unit.nva.publication.model.business.importcandidate;

import static java.util.Objects.nonNull;
import java.util.Collection;
import java.util.Collections;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.UnusedAssignment")
@JacocoGenerated
public record ScopusAffiliation(AffiliationIdentifier identifier,
                                Collection<String> names,
                                String text,
                                Country country,
                                Address address) implements ScopusOrganization {

    public ScopusAffiliation {
        names = nonNull(names) ? Collections.unmodifiableCollection(names) : Collections.emptyList();
    }

    public static ScopusAffiliation emptyAffiliation() {
        return new ScopusAffiliation(null, null, null, null, null);
    }
}
