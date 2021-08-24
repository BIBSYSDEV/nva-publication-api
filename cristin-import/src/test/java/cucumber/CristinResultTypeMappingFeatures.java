package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import no.unit.nva.model.pages.Pages;

public class CristinResultTypeMappingFeatures {

    private final ScenarioContext scenarioContext;

    public CristinResultTypeMappingFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a valid Cristin Result with secondary category {string}")
    public void valid_cristin_result_with_secondary_category(String secondaryCategory) {
        this.scenarioContext.newCristinEntry(() -> CristinDataGenerator.randomObject(secondaryCategory));
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

    @Then("the NVA Resource has a Content type of type {string}")
    public void theNvaResourceHasAContentTypeOfType(String expectedType) {
        PublicationInstance<? extends Pages> instance = this.scenarioContext.getNvaEntry()
                .getEntityDescription()
                .getReference()
                .getPublicationInstance();
        JournalArticle journalArticle = (JournalArticle) instance;
        JournalArticleContentType contentType = journalArticle.getContentType();
        String actuallType = contentType.getValue();
        assertThat(actuallType, is(equalTo(expectedType)));
    }
}
