package no.unit.nva.importcandidate;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import no.unit.nva.model.Identity;
import no.unit.nva.model.role.RoleType;

@SuppressWarnings("PMD.UnusedAssignment")
@JsonTypeInfo(use = Id.NAME, property = "type")
public record ImportContributor(Identity identity, Collection<Affiliation> affiliations, RoleType role,
                                Integer sequence, boolean correspondingAuthor) {

    public ImportContributor {
        affiliations = nonNull(affiliations) ? List.copyOf(affiliations) : Collections.emptyList();
    }
}
