package cucumber;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_CHANNEL_REGISTRY_V2;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.net.URI;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.instancetypes.media.MediaFeatureArticle;
import nva.commons.core.paths.UriWrapper;

public class JournalFeatures {

    private static final String EMPTY_JOURNAL_TITLE = null;
    private final ScenarioContext scenarioContext;

    public JournalFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("that the Cristin Result has a non empty Journal Publication")
    public void thatTheCristinResultHasANonEmptyJournalPublication() {
        assertThat(this.scenarioContext.getCristinEntry().getJournalPublication(), is(not(nullValue())));
    }

    @Given("the Journal Publication has a \"journalName\" entry equal to {string}")
    public void theJournalPublicationHasApublisherNameEntryEqualTo(String publisherName) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .getJournal()
            .setJournalTitle(publisherName);
    }

    @Given("the Journal Publication has a \"issn\" entry equal to {string}")
    public void theJournalPublicationHasAIssnEntryEqualTo(String issnNumber) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .getJournal()
            .setIssn(issnNumber);
    }

    @Then("the Nva Resource has a PublicationContext with printISSN equal to {string}")
    public void theNvaResourceHasAPublicationContextWithPrintIssnEqualTo(String expectedIssn) {
        var context = scenarioContext
                          .getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        var journal = (UnconfirmedJournal) context;
        var actualIssn = journal.getPrintIssn();
        assertThat(actualIssn, is(equalTo(expectedIssn)));
    }

    @Given("the Journal Publication has a \"issnOnline\" entry equal to {string}")
    public void theJournalPublicationHasAIssnOnlineEntryEqualTo(String issnNumber) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .getJournal()
            .setIssnOnline(issnNumber);
    }

    @Then("the Nva Resource has a PublicationContext with onlineIssn equal to {string}")
    public void theNvaResourceHasAPublicationContextWithOnlineIssnEqualTo(String expectedIssn) {
        var context = scenarioContext
                          .getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        var journal = (UnconfirmedJournal) context;
        var actualIssn = journal.getOnlineIssn();
        assertThat(actualIssn, is(equalTo(expectedIssn)));
    }

    @Then("the Nva Resource has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalArticle = (JournalArticle) instance;
        var actualPagesBegin = journalArticle.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource has a PublicationContext with title equal to {string}")
    public void theNvaResourceHasAPublicationContextWithTitleEqualTo(String expectedTitle) {
        var context = scenarioContext
                          .getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        var journal = (UnconfirmedJournal) context;
        var actualTitle = journal.getTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
    }

    @Given("the Journal Publication has a \"pagesBegin\" entry equal to {string}")
    public void theJournalPublicationHasAPagesBeginEntryEqualTo(String pagesBegin) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .setPagesBegin(pagesBegin);
    }

    @Given("the Journal Publication has a \"pagesEnd\" entry equal to {string}")
    public void theJournalPublicationHasAPagesEndEntryEqualTo(String pagesEnd) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .setPagesEnd(pagesEnd);
    }

    @Then("the Nva Resource has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalArticle = (JournalArticle) instance;
        var actualPagesEnd = journalArticle.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource has a PublicationContext with volume equal to {string}")
    public void theNvaResourceHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalArticle = (JournalArticle) instance;
        var actualVolume = journalArticle.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Given("the Journal Publication has a \"volume\" entry equal to {string}")
    public void theJournalPublicationHasAVolumeEntryEqualTo(String volume) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .setVolume(volume);
    }

    @Given("that the Journal Article entry has an empty \"publisherName\" field")
    public void thatTheJournalArticleEntryHasAnEmptyPublisherNameField() {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .getJournal()
            .setJournalTitle(EMPTY_JOURNAL_TITLE);
    }

    @Given("the Journal Publication has a \"doi\" entry equal to {string}")
    public void theJournalPublicationHasADoiEntryEqualTo(String doi) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .setDoi(doi);
    }

    @Given("the Journal Publication has a reference to an NSD journal or publisher with identifier {int}")
    public void theJournalPublicationHasAReferenceToAnNsdJournalOrPublisherWithIdentifier(int nsdCode) {
        scenarioContext.getCristinEntry().getJournalPublication().getJournal().setNsdCode(nsdCode);
    }

    @Given("the Journal Publication has publishing year equal to {int}")
    public void theJournalPublicationHasPublishingYearEqualTo(int yearPublishedInJournal) {
        scenarioContext.getCristinEntry().setPublicationYear(yearPublishedInJournal);
    }

    @Given("the year the Cristin Result was published is equal to {int}")
    public void theYearTheCristinResultWasPublishedIsEqualTo(int publicationYear) {
        scenarioContext.getCristinEntry().setPublicationYear(publicationYear);
    }

    @Then("the NVA Resource has a Reference object with a journal URI that points to NVAs NSD proxy")
    public void theNvaResourceHasAReferenceObjectWithAJournalUriThatPointsToNvaNsdProxy() {
        var journal = extractJournal();
        var uriString = journal.getId().toString();
        var nsdProxyUri = UriWrapper.fromUri(NVA_API_DOMAIN).addChild(NVA_CHANNEL_REGISTRY_V2).getUri().toString();
        assertThat(uriString, containsString(nsdProxyUri));
    }

    @Then("the Journal URI specifies the Journal by the NSD ID {string} and the year {int}.")
    public void theJournalUriSpecifiesTheJournalByTheNsdIdAndTheYear(String pid, Integer journalYear) {
        var journal = extractJournal();
        var uriString = journal.getId().toString();
        assertThat(uriString, containsString(pid));
        assertThat(uriString, containsString(journalYear.toString()));
    }

    @Then("the Nva Resource has a Reference object with doi equal to {string}")
    public void theNvaResourceHasAReferenceObjectWithDoiEqualTo(String expectedDoi) {
        var reference = scenarioContext
                            .getNvaEntry()
                            .getEntityDescription()
                            .getReference();
        var actualDoi = reference.getDoi();
        assertThat(actualDoi, is(equalTo(URI.create(expectedDoi))));
    }

    @Then("the Nva Resource, MediaFeatureArticle, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceMediaFeatureArticleHasAPublicationContextWithPagesBeginEqualTo(
        String expectedPagesBegin) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var mediaFeatureArticle = (MediaFeatureArticle) instance;
        var actualPagesBegin = mediaFeatureArticle.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, MediaFeatureArticle, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceFeatureArticleHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var mediaFeatureArticle = (MediaFeatureArticle) instance;
        var actualPagesEnd = mediaFeatureArticle.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, MediaFeatureArticle, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceFeatureArticleHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var mediaFeatureArticle = (MediaFeatureArticle) instance;
        var actualVolume = mediaFeatureArticle.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Then("the Nva Resource, JournalLetter, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceJournalLetterHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalLetter = (JournalLetter) instance;
        var actualPagesBegin = journalLetter.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, JournalLetter, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceJournalLetterHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalLetter = (JournalLetter) instance;
        var actualPagesEnd = journalLetter.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, JournalLetter, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceJournalLetterHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalLetter = (JournalLetter) instance;
        var actualVolume = journalLetter.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Then("the Nva Resource, JournalReview, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceJournalReviewHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalReview = (JournalReview) instance;
        var actualPagesBegin = journalReview.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, JournalReview, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceJournalReviewHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalReview = (JournalReview) instance;
        var actualPagesEnd = journalReview.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, JournalReview, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceJournalReviewHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalReview = (JournalReview) instance;
        var actualVolume = journalReview.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Then("the Nva Resource, JournalLeader, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceJournalLeaderHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalLeader = (JournalLeader) instance;
        var actualPagesBegin = journalLeader.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, JournalLeader, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceJournalLeaderHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalLeader = (JournalLeader) instance;
        var actualPagesEnd = journalLeader.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, JournalLeader, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceJournalLeaderHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalLeader = (JournalLeader) instance;
        var actualVolume = journalLeader.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Then("the Nva Resource, JournalCorrigendum, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceJournalCorrigendumHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalCorrigendum = (JournalCorrigendum) instance;
        var actualPagesBegin = journalCorrigendum.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, JournalCorrigendum, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceJournalCorrigendumHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalCorrigendum = (JournalCorrigendum) instance;
        var actualPagesEnd = journalCorrigendum.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, JournalCorrigendum, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceJournalCorrigendumHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalCorrigendum = (JournalCorrigendum) instance;
        var actualVolume = journalCorrigendum.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @And("the Journal Publication has a \"issue\" entry equal to {string}")
    public void theJournalPublicationHasAIssueEntryEqualTo(String issue) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .setIssue(issue);
    }

    @Then("the Nva Resource, MediaFeatureArticle, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceFeatureArticleHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var mediaFeatureArticle = (MediaFeatureArticle) instance;
        var actualIssue = mediaFeatureArticle.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, Journal Article, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalArticleHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalArticle = (JournalArticle) instance;
        var actualIssue = journalArticle.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, JournalCorrigendum, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalCorrigendumHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalCorrigendum = (JournalCorrigendum) instance;
        var actualIssue = journalCorrigendum.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, Journal Leader, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalLeaderHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalLeader = (JournalLeader) instance;
        var actualIssue = journalLeader.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, Journal Letter, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalLetterHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalLetter = (JournalLetter) instance;
        var actualIssue = journalLetter.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, Journal Review, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalReviewHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        var instance = scenarioContext
                           .getNvaEntry()
                           .getEntityDescription()
                           .getReference()
                           .getPublicationInstance();
        var journalReview = (JournalReview) instance;
        var actualIssue = journalReview.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @And("the Publisher URI contains the NSD pid code {string} and the publication year {int}")
    public void thePublisherUriContainsTheNsdPidCodeAndThePublicationYear(String pid, Integer year) {
        Journal journal = extractJournal();
        String uriString = journal.getId().toString();
        assertThat(uriString, containsString(pid));
        assertThat(uriString, containsString("publisher"));
        assertThat(uriString, containsString(year.toString()));
    }

    private Journal extractJournal() {
        var publicationContext = scenarioContext.getNvaEntry()
                                     .getEntityDescription()
                                     .getReference()
                                     .getPublicationContext();
        return (Journal) publicationContext;
    }
}
