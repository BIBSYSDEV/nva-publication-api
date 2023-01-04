package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.mapper.CristinBookOrReportPartMetadata;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;

public class ChapterArticleFeatures {

    private final ScenarioContext scenarioContext;

    public ChapterArticleFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("the Cristin Result has a page range from {string} to {string}.")
    public void theCristinResultHasAPageRangeFromTo(String start, String end) {
        this.scenarioContext.getCristinEntry().getBookOrReportPartMetadata().setPagesStart(start);
        this.scenarioContext.getCristinEntry().getBookOrReportPartMetadata().setPagesEnd(end);
    }

    @Then("the NVA Resource has a PublicationInstance with pages starting at {string} and ending at {string}")
    public void theNvaResourceHasAPublicationInstanceWithPagesStartingAtAndEndingAt(
        String expectedStart, String expectedEnd) {
        PublicationInstance<?> instance = scenarioContext.getNvaEntry()
                                              .getEntityDescription()
                                              .getReference()
                                              .getPublicationInstance();
        ChapterArticle chapterArticle = (ChapterArticle) instance;
        String actuallStart = chapterArticle.getPages().getBegin();
        String actuallEnd = chapterArticle.getPages().getEnd();
        assertThat(actuallStart, is(equalTo(expectedStart)));
        assertThat(actuallEnd, is(equalTo(expectedEnd)));
    }

    @Given("the Cristin Result has a non empty Book Report Part")
    public void theCristinResultHasANonEmptyBookReportPart() {
        CristinBookOrReportPartMetadata cristinBookOrReportPartMetadata =
            CristinBookOrReportPartMetadata.builder().build();
        this.scenarioContext.getCristinEntry().setBookOrReportPartMetadata(cristinBookOrReportPartMetadata);
    }

    @Then("the Chapter Article has a \"isPeerReviewed\" equal to True")
    public void theChapterArticleHasAIsPeerReviewedEqualToTrue() {
        PublicationInstance<?> context = scenarioContext.getNvaEntry()
                                             .getEntityDescription()
                                             .getReference()
                                             .getPublicationInstance();
        ChapterArticle chapterArticle = (ChapterArticle) context;
        assertThat(chapterArticle.isPeerReviewed(), is(true));
    }
}
