package no.unit.nva.publication.model.storage;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Contributor.Builder;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Contribution;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContributionDaoTest extends ResourcesLocalTest {
    
    public static final URI SAMPLE_ORG = URI.create("https://example.org/123");
    public static final String SAMPLE_OWNER_USERNAME = "some@owner";
    public static final UserInstance SAMPLE_OWNER = UserInstance.create(SAMPLE_OWNER_USERNAME, SAMPLE_ORG);
    public static final ResourceOwner RANDOM_RESOURCE_OWNER =
         new ResourceOwner(new Username(SAMPLE_OWNER.getUsername()), SAMPLE_OWNER.getOrganizationUri());
    private ResourceService resourceService;
    
    @BeforeEach
    public void initialize() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
    }


    @Test
    void shouldNotThrowWhenInsertingPublicationWithContributors() throws ApiGatewayException {
        var contributors = List.of(
            new Contributor.Builder().build()
        );
        insertSamplePublication(contributors);
    }

    @Test
    void daoObjectShouldHaveCorrectPartitionAndSortKeys() {
        Resource.builder().withIdentifier(SortableIdentifier.next()).build();
        var resource =
            Resource.builder().withIdentifier(SortableIdentifier.next()).withResourceOwner(Owner.fromResourceOwner(RANDOM_RESOURCE_OWNER)).build();
        var contributor = new Contributor.Builder().build();
        var contribution = Contribution.create(resource, contributor);
        var dao = new ContributionDao(contribution);

        assertThat(dao.getPrimaryKeySortKey(),
                   is(equalTo(expectedPrimarySortKey(resource, contribution))));
    }

    @Test
    public void createInsertionTransactionRequestShouldThrow() {
        var publication = randomPublication();
        var dao = emptyContribution(Resource.fromPublication(publication));
        assertThrows(UnsupportedOperationException.class, dao::createInsertionTransactionRequest);
    }

    @Test
    public void updateExistingEntryShouldThrow() {
        var publication = randomPublication();
        var dao = emptyContribution(Resource.fromPublication(publication));
        assertThrows(UnsupportedOperationException.class, () -> dao.updateExistingEntry(null));
    }

    private ContributionDao emptyContribution(Resource resource) {
        var contributor = new Builder().build();
        var contribution = Contribution.create(resource, contributor);
        return new ContributionDao(contribution);
    }


    private String expectedPrimarySortKey(Resource publication, Contribution contribution) {
        return "Resource"
               + KEY_FIELDS_DELIMITER
               + publication.getIdentifier()
               + KEY_FIELDS_DELIMITER
               + "Contribution"
               + KEY_FIELDS_DELIMITER
               + contribution.getIdentifier();
    }

    private void insertSamplePublication(List<Contributor> contributors)
        throws ApiGatewayException {

        var entityDescription = new EntityDescription.Builder()
                                    .withContributors(contributors)
                                    .build();

        var publication = PublicationGenerator.randomPublication().copy()
                              .withEntityDescription(entityDescription)
                              .build();


        resourceService.createPublication(UserInstance.create(ResourceOwner.OWNER, randomUri()), publication);
    }
}