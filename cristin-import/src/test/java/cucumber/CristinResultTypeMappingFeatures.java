package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.CristinDataGenerator;

public class CristinResultTypeMappingFeatures {

    private final ScenarioContext scenarioContext;

    public CristinResultTypeMappingFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a valid Cristin Result with secondary category {string}")
    public void valid_cristin_result_with_secondary_category(String secondaryCategory) {
        this.scenarioContext.newCristinEntry(() -> CristinDataGenerator.randomObject(secondaryCategory));
    }

    @Given("a random book")
    public void aRandomBook() {
        this.scenarioContext.newCristinEntry(CristinDataGenerator::randomBook);
    }

    @Then("the NVA Resource has a Publication Instance of type {string}")
    public void theNvaResourceIsAnInstanceOf(String type) {
        String publicationType = this.scenarioContext.getNvaEntry()
                                     .getEntityDescription()
                                     .getReference()
                                     .getPublicationInstance()
                                     .getInstanceType();
        assertThat(publicationType, is(equalTo(type)));
    }

    @Given("the Cristin Result has a value for the date when it was reported in NVI.")
    public void theCristinResultHasAValueForTheDateWhenItWasReportedInNVI() {
        this.scenarioContext.getCristinEntry().setYearReported(2020);
    }
}
