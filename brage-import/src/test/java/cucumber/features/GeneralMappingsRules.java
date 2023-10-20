package cucumber.features;

import cucumber.ScenarioContext;
import io.cucumber.java.en.When;

public class GeneralMappingsRules {

    private final ScenarioContext scenarioContext;

    public GeneralMappingsRules(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @When("the nva publications are merged")
    public void theNvaPublicationIsUpdated() {
        scenarioContext.mergePublications();
    }
}
