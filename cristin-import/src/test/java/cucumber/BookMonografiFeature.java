package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Set;
import no.unit.nva.model.AdditionalIdentifier;

public class BookMonografiFeature {

    private final ScenarioContext scenarioContext;

    public BookMonografiFeature(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a valid Cristin Entry")
    public void validCristinEntry() {
        this.scenarioContext.newCristinEntry();
    }

    @Given("the Cristin Entry has id equal to {int}")
    public void theCristinEntryHasIdEqualTo(int id) {

        this.scenarioContext.getCristinEntry().withId(id);
    }

    @When("the Cristin Entry is converted to an NVA entry")
    public void is_converted_to_an_nva_entry() {
        scenarioContext.newNvaEntry();
    }

    @Then("the NVA Entry says {string} too.")
    public void says_too(String message) {
        assertThat(scenarioContext.getNvaEntry().getOwner(), is(equalTo(message)));
    }

    @Then("the NVA Entry has an additional identifier with key {string} and value {int}")
    public void theNvaEntryHasAnAdditionalIdentifierWithKeyAndValue(String cristinAdditionalIdentifierKey,
                                                                    int expectedCristinId) {
        Set<AdditionalIdentifier> actualAdditionalIdentifiers =
            scenarioContext.getNvaEntry().getAdditionalIdentifiers();
        AdditionalIdentifier expectedIdentifier =
            new AdditionalIdentifier(cristinAdditionalIdentifierKey, Integer.toString(expectedCristinId));

        assertThat(actualAdditionalIdentifiers, contains(expectedIdentifier));
    }
}
