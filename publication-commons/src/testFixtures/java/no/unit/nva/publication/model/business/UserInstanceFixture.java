package no.unit.nva.publication.model.business;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import java.util.List;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.testutils.RandomDataGenerator;

public class UserInstanceFixture {
    public static UserInstance getDegreeAndFileCuratorFromPublication(Publication publication) {
        var contributor = publication.getEntityDescription().getContributors().getFirst();
        var topLevelOrgCristinId =
            contributor.getAffiliations().stream().map(Organization.class::cast).findFirst().orElseThrow().getId();
        return new UserInstance(RandomDataGenerator.randomString(),
                                publication.getPublisher().getId(),
                                topLevelOrgCristinId,
                                null, null, List.of(MANAGE_DEGREE, MANAGE_RESOURCE_FILES,
                                                    MANAGE_RESOURCES_STANDARD), UserClientType.INTERNAL, null);
    }
}
