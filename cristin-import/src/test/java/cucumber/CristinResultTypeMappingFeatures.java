package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.BookMonographContentType;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType;
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

    @Then("the NVA JournalArticle Resource has a Content type of type {string}")
    public void theNvaJournalArticleResourceHasAContentTypeOfType(String expectedType) {
        PublicationInstance<? extends Pages> instance = this.scenarioContext.getNvaEntry()
                .getEntityDescription()
                .getReference()
                .getPublicationInstance();
        JournalArticle journalArticle = (JournalArticle) instance;
        JournalArticleContentType contentType = journalArticle.getContentType();
        String actuallType = contentType.getValue();
        assertThat(actuallType, is(equalTo(expectedType)));
    }

    @Then("the NVA BookMonograph Resource has a Content type of type {string}")
    public void theNvaBookMonographResourceHasAContentTypeOfType(String expectedType) {
        PublicationInstance<? extends Pages> instance = this.scenarioContext.getNvaEntry()
                .getEntityDescription()
                .getReference()
                .getPublicationInstance();
        BookMonograph bookMonograph = (BookMonograph) instance;
        BookMonographContentType contentType = bookMonograph.getContentType();
        String actuallType = contentType.getValue();
        assertThat(actuallType, is(equalTo(expectedType)));
    }

    @Then("the NVA ChapterArticle Resource has a Content type of type {string}")
    public void theNvaChapterArticleResourceHasAContentTypeOfType(String expectedContentType) {
        PublicationInstance<? extends Pages> instance = this.scenarioContext.getNvaEntry()
                .getEntityDescription()
                .getReference()
                .getPublicationInstance();
        ChapterArticle chapterArticle = (ChapterArticle) instance;
        ChapterArticleContentType contentType = chapterArticle.getContentType();
        String actualType = contentType.getValue();
        assertThat(actualType, is(equalTo(expectedContentType)));
    }

    @Given("the Cristin Result has a value for the date when it was reported in NVI.")
    public void theCristinResultHasAValueForTheDateWhenItWasReportedInNVI() {
        this.scenarioContext.getCristinEntry().setYearReported(2020);
    }
}
