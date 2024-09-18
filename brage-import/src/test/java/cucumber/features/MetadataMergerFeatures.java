package cucumber.features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import cucumber.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.List;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.Contributor;

public class MetadataMergerFeatures {

    private final ScenarioContext scenarioContext;

    public MetadataMergerFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("the NVA publication has an additionalIdentifier with type {string} and value {string}")
    public void theNvaPublicationHasAnAdditionalIdentifierWithTypeAndValue(String type, String value) {
        var expectedAdditionalIdentifier = new AdditionalIdentifier(type, value);
        var additionalIdentifiers = scenarioContext.getMergedPublication().getAdditionalIdentifiers();
        assertThat(additionalIdentifiers, hasItem(expectedAdditionalIdentifier));
    }

    @Given("a brage publication with an creator with properties:")
    public void aBragePublicationWithAnCreatorWithProperties(Contributor contributor) {
        var bragePublication = scenarioContext.getBragePublication().publication();
        bragePublication.getEntityDescription().setContributors(List.of(contributor));
    }

    @And("a NVA publication without contributors")
    public void aNVAPublicationWithoutContributors() {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.getEntityDescription().setContributors(List.of());
    }

    @Then("the NVA publication has a contributor with properties:")
    public void theNVAPublicationHasAContributorWithProperties(Contributor contributor) {
        var publication = scenarioContext.getMergedPublication();
        assertThat(publication.getEntityDescription().getContributors(), contains(contributor));
    }
}
