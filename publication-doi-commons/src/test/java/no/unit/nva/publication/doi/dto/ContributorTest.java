package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.github.javafaker.Faker;
import no.unit.nva.publication.doi.dynamodb.dao.Identity.Builder;
import org.junit.jupiter.api.Test;

class ContributorTest {

    @Test
    void testDaoConstructor() {
        var faker = new Faker();
        var name = faker.superhero().name();
        var arpId = faker.number().digits(10);
        var orcId = faker.number().digits(10);

        var contributor = new Contributor(new Builder()
            .withName(name)
            .withOrcId(orcId)
            .withArpId(arpId)
            .build());
        assertThat(contributor.getId(), nullValue());
        assertThat(contributor.getArpId(), is(equalTo(arpId)));
        assertThat(contributor.getName(), is(equalTo(name)));
    }
}