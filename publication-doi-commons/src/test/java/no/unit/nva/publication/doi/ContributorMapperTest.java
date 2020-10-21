package no.unit.nva.publication.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import com.github.javafaker.Faker;
import java.util.Collections;
import java.util.List;
import no.unit.nva.publication.doi.dto.Contributor;
import no.unit.nva.publication.doi.dynamodb.dao.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContributorMapperTest {

    private Faker faker;

    private List<Identity> getContributorIdentities() {
        Identity.Builder builder = new Identity.Builder();
        builder.withArpId(faker.number().digits(10));
        builder.withOrcId(faker.number().digits(10));
        builder.withName(faker.superhero().name());
        return Collections.singletonList(builder.build());
    }

    @BeforeEach
    void configure() {
        faker = new Faker();
    }

    @Test
    void fromIdentityDaoThenReturnContributorDto() {
        var contributorIdentities = getContributorIdentities();
        var firstContributorIdentity = contributorIdentities.get(0);
        var firstExpectedContributor = new Contributor(null, firstContributorIdentity.getArpId(),
            firstContributorIdentity.getName());
        assertThat(ContributorMapper.fromIdentityDaos(contributorIdentities), hasItem(firstExpectedContributor));
    }
}