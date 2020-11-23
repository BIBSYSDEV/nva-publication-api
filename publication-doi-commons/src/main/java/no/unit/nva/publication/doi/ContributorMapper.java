package no.unit.nva.publication.doi;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.publication.doi.dto.Contributor;
import no.unit.nva.publication.doi.dynamodb.dao.Identity;

public final class ContributorMapper {

    private ContributorMapper() {
    }

    public static Contributor fromIdentityDao(Identity contributorIdentityDao) {
        return createContributor(contributorIdentityDao);
    }

    /**
     * Construct Contributor DTOs from Identity DAOs.
     *
     * @param contributorIdentities list of (contributor) identities to convert.
     * @return list of contributor DTOs
     */
    public static List<Contributor> fromIdentityDaos(List<Identity> contributorIdentities) {
        var contributors = new ArrayList<Contributor>();
        for (Identity contributorIdentity : contributorIdentities) {
            contributors.add(createContributor(contributorIdentity));
        }
        return contributors;
    }

    private static Contributor createContributor(Identity identity) {
        return Contributor.builder().withArpId(identity.getArpId()).withName(identity.getName()).build();
    }
}
