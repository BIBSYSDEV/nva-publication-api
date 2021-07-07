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

    @Then("the NVA Resource has a Reference with PublicationInstance of Type {string}")
    public void the_nva_resource_has_a_reference_with_publication_instance_of_type(String publicationInstanceType) {
        String publicationType = this.scenarioContext.getNvaEntry()
                                     .getEntityDescription()
                                     .getReference()
                                     .getPublicationInstance()
                                     .getInstanceType();
        assertThat(publicationType, is(equalTo(publicationInstanceType)));
    }
}
