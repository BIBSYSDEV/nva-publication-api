package cucumber.features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import cucumber.ScenarioContext;
import io.cucumber.java.en.Then;
import no.unit.nva.model.AdditionalIdentifier;

public class MetadataMergerFeatures {

    private final ScenarioContext scenarioContext;

    public MetadataMergerFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("the NVA publication has an additionalIdentifier with type {string} and value {string}")
    public void theNVAPublicationHasAnAdditionalIdentifierWithTypeAndValue(String type, String value) {
        var expectedAdditionalIdentifier = new AdditionalIdentifier(type, value);
        var additionalIdentifiers = scenarioContext.getMergedPublication().getAdditionalIdentifiers();
        assertThat(additionalIdentifiers, hasItem(expectedAdditionalIdentifier));
    }
}
