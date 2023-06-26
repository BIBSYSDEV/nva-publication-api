package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Clock;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContributionTest extends ResourcesLocalTest {
    private ResourceService resourceService;
    private UserInstance USER_INSTANCE = UserInstance.create(ResourceOwner.OWNER, randomUri());

    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
    }

    @Test
    public void toPublicationShouldThrow() throws ApiGatewayException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication);
        var contributor = randomContributor();
        var contribution = Contribution.create(resource, contributor);

        resourceService.createPublication(USER_INSTANCE, publication);

        assertThrows(UnsupportedOperationException.class, () -> contribution.toPublication(null));
    }

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }

}