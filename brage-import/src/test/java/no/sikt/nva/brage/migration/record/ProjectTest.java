package no.sikt.nva.brage.migration.record;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.net.URI;
import java.util.Map;
import no.unit.nva.model.funding.FundingBuilder;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class ProjectTest {

    @Test
    void shouldCreateVerifiedFundingWhenFundingSourceIsNfr() {
        var source = "NFR";
        var identifier = randomString();
        var project = new Project(identifier, randomString(),
                                  new FundingSource(source, Map.of("nb", randomString())));
        var funding = project.toFunding();
        var expectedFunding = new FundingBuilder()
                                  .withSource(constructExpectedSource(source))
                                  .withId(constructExpectedId(identifier))
                                  .withIdentifier(identifier)
                                  .build();

        assertThat(funding, is(equalTo(expectedFunding)));
    }

    @Test
    void shouldCreateUnconfirmedFundingWhenFundingSourceIsPresentButNotNfrFunding() {
        var source = "EU";
        var identifier = randomString();
        var project = new Project(identifier, randomString(),
                                  new FundingSource(source, Map.of("nb", randomString())));
        var funding = project.toFunding();
        var expectedFunding = new FundingBuilder()
                                  .withSource(constructExpectedSource(source))
                                  .withIdentifier(identifier)
                                  .build();

        assertThat(funding, is(equalTo(expectedFunding)));
    }

    @Test
    void shouldCreateUnconfirmedFundingWithoutSourceWhenFundingsSourceIsMissing() {
        var identifier = randomString();
        var project = new Project(identifier, randomString(), null);
        var funding = project.toFunding();
        var expectedFunding = new FundingBuilder()
                                  .withLabels(Map.of("nb", project.name()))
                                  .withIdentifier(identifier)
                                  .build();


        assertThat(funding.getIdentifier(), is(equalTo(expectedFunding.getIdentifier())));
        assertThat(funding.getLabels(), is(equalTo(expectedFunding.getLabels())));
    }

    private URI constructExpectedId(String identifier) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("verified-funding")
                   .addChild("nfr")
                   .addChild(identifier)
                   .getUri();
    }

    private URI constructExpectedSource(String source) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("cristin")
                   .addChild("funding-sources")
                   .addChild(source)
                   .getUri();
    }
}