package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.PublicationDate;

public class GeneralMappingRules {

    private final ScenarioContext scenarioContext;

    public GeneralMappingRules(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a valid Cristin Result")
    public void validCristinEntry() {
        this.scenarioContext.newCristinEntry();
    }

    @Given("the Cristin Result has id equal to {int}")
    public void theCristinEntryHasIdEqualTo(int id) {

        this.scenarioContext.getCristinEntry().setId(id);
    }

    @Given("the Cristin Result has an non null array of CristinTitles")
    public void theCristinEntryHasAnNonNullArrayOfCristinTitles() {
        scenarioContext.addEmptyCristinTitle();
    }

    @Given("the CristinTitle array has an entry")
    public void theCristinTitleArrayHasASingleEntry() {
        scenarioContext.addCristinTitle();
    }

    @Given("the CristinTitle entry has title text equal to {string}")
    public void theCristinTitleEntryHasTitleTextEqualTo(String titleText) {
        scenarioContext.getLatestCristinTitle().setTitle(titleText);
    }

    @Given("the CristinTitle entry has original status annotation {string}")
    public void theCristinTitleEntryHasOriginalStatusAnnotation(String statusOriginal) {
        scenarioContext.getLatestCristinTitle().setStatusOriginal(statusOriginal);
    }

    @Given("the Cristin Result has publication year {string}")
    public void theCristinEntryHasPublicationYear(String publicationYear) {
        scenarioContext.getCristinEntry().setPublicationYear(publicationYear);
    }

    @Given("that Cristin Result has created date equal to the local date {string}")
    public void thatCristinEntryHasCreatedDateEqualToTheLocalDate(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString);
        scenarioContext.getCristinEntry().setEntryCreationDate(localDate);
    }

    @When("the Cristin Result is converted to an NVA Resource")
    public void is_converted_to_an_nva_entry() {
        scenarioContext.convertToNvaEntry();
    }

    @Then("the NVA Resource has an additional identifier with key {string} and value {int}")
    public void theNvaEntryHasAnAdditionalIdentifierWithKeyAndValue(String cristinAdditionalIdentifierKey,
                                                                    int expectedCristinId) {
        Set<AdditionalIdentifier> actualAdditionalIdentifiers =
            scenarioContext.getNvaEntry().getAdditionalIdentifiers();
        AdditionalIdentifier expectedIdentifier =
            new AdditionalIdentifier(cristinAdditionalIdentifierKey, Integer.toString(expectedCristinId));

        assertThat(actualAdditionalIdentifiers, contains(expectedIdentifier));
    }

    @Then("the NVA Resource has an EntityDescription with mainTitle {string}")
    public void theNVAResourceHasAnEntityDescriptionWithMainTitle(String expectedTitle) {
        String actualTitle = scenarioContext.getNvaEntry().getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
    }

    @Then("the NVA Resource has a Publication Date with year equal to {string}, month equal to null and "
          + "day equal to null")
    public void theNvaResourceHasPublicationDateWithTheCristinYear(String expectedPublicationYear) {
        PublicationDate actualDate = scenarioContext.getNvaEntry().getEntityDescription().getDate();
        assertThat(actualDate.getYear(), is(equalTo(expectedPublicationYear)));
        assertThat(actualDate.getMonth(), is(nullValue()));
        assertThat(actualDate.getDay(), is(nullValue()));
    }

    @Then("the NVA Resource has a Creation Date equal to {string}")
    public void theNVAResourceHasACreationDateEqualTo(String expectedIsoInstant) {
        Instant expectedInstant = Instant.parse(expectedIsoInstant);
        assertThat(scenarioContext.getNvaEntry().getCreatedDate(), is(equalTo(expectedInstant)));
    }
}
