package no.unit.nva.publication.doi;

import java.util.List;
import java.util.stream.Collectors;
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
        return contributorIdentities.stream()
            .map(ContributorMapper::createContributor)
            .collect(Collectors.toList());
    }

    private static Contributor createContributor(Identity identity) {
        return new Contributor.Builder().withArpId(identity.getArpId()).withName(identity.getName()).build();
    }
}
