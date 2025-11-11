package no.unit.nva.importcandidate;

import static java.util.Objects.nonNull;
import java.util.Collection;
import java.util.Collections;
import no.unit.nva.model.Identity;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.UnusedAssignment")
@JacocoGenerated
public record ImportContributor(Identity identity, Collection<ImportOrganization> affiliations, RoleType role,
                                Integer sequence, boolean correspondingAuthor) {

    public ImportContributor {
        affiliations = nonNull(affiliations) ? Collections.unmodifiableCollection(affiliations) : Collections.emptyList();
    }
}
