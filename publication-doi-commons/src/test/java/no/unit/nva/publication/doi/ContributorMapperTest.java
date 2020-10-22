package no.unit.nva.publication.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.github.javafaker.Faker;
import java.util.Collections;
import java.util.List;
import no.unit.nva.publication.doi.dto.Contributor;
import no.unit.nva.publication.doi.dynamodb.dao.Identity;
import no.unit.nva.publication.doi.dynamodb.dao.Identity.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContributorMapperTest {

    private Faker faker;

    @BeforeEach
    void configure() {
        faker = new Faker();
    }

    @Test
    void fromIdentityDaoThenReturnContributorDto() {
        var dao = getIdentityBuilderDao().build();
        var expectedDto = new Contributor(null, dao.getArpId(), dao.getName());
        assertThat(ContributorMapper.fromIdentityDao(dao), is(equalTo(expectedDto)));
    }

    @Test
    void fromIdentityDaosThenReturnListOfContributorDtos() {
        var contributorIdentities = getContributorIdentities();
        var firstContributorIdentity = contributorIdentities.get(0);
        var firstExpectedContributor = new Contributor(null, firstContributorIdentity.getArpId(),
            firstContributorIdentity.getName());
        assertThat(ContributorMapper.fromIdentityDaos(contributorIdentities), hasItem(firstExpectedContributor));
    }

    private List<Identity> getContributorIdentities() {
        var dao = getIdentityBuilderDao().build();
        return Collections.singletonList(dao);
    }

    private Identity.Builder getIdentityBuilderDao() {
        Builder builder = new Builder();
        builder.withArpId(faker.number().digits(10));
        builder.withOrcId(faker.number().digits(10));
        builder.withName(faker.superhero().name());
        return builder;
    }
}