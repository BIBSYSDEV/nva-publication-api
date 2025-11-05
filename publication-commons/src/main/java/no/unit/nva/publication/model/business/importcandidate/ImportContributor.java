package no.unit.nva.publication.model.business.importcandidate;

import java.util.List;
import no.unit.nva.model.Identity;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public record ImportContributor(Identity identity,
                                List<ImportOrganization> affiliations,
                                RoleType role,
                                Integer sequence,
                                boolean correspondingAuthor) {

}
