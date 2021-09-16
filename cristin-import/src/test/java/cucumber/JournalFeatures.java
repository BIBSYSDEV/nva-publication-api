package cucumber;

import static no.unit.nva.cristin.CristinDataGenerator.randomIssn;
import static no.unit.nva.cristin.CristinDataGenerator.randomString;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NSD_PROXY_PATH;
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
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.s3imports.UriWrapper;

public class JournalFeatures {

    private final ScenarioContext scenarioContext;

    private static final String EMPTY_JOURNAL_TITLE = null;

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
        PublicationContext context = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationContext();
        UnconfirmedJournal journal = (UnconfirmedJournal) context;
        String actualIssn = journal.getPrintIssn();
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
        PublicationContext context = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationContext();
        UnconfirmedJournal journal = (UnconfirmedJournal) context;
        String actualIssn = journal.getOnlineIssn();
        assertThat(actualIssn, is(equalTo(expectedIssn)));
    }

    @Then("the Nva Resource has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalArticle journalArticle = (JournalArticle) instance;
        String actualPagesBegin = journalArticle.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource has a PublicationContext with title equal to {string}")
    public void theNvaResourceHasAPublicationContextWithTitleEqualTo(String expectedTitle) {
        PublicationContext context = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationContext();
        UnconfirmedJournal journal = (UnconfirmedJournal) context;
        String actualTitle = journal.getTitle();
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
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalArticle journalArticle = (JournalArticle) instance;
        String actualPagesEnd = journalArticle.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource has a PublicationContext with volume equal to {string}")
    public void theNvaResourceHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalArticle journalArticle = (JournalArticle) instance;
        String actualVolume = journalArticle.getVolume();
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
        Journal journal = extractJournal();
        String uriString = journal.getId().toString();
        String nsdProxyUri = new UriWrapper(NVA_API_DOMAIN).addChild(NSD_PROXY_PATH).getUri().toString();
        assertThat(uriString, containsString(nsdProxyUri));
    }

    private Journal extractJournal() {
        PublicationContext publicationContext = scenarioContext.getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationContext();
        return (Journal) publicationContext;
    }

    @Then("the Journal URI specifies the Journal by the NSD ID {int} and the year {int}.")
    public void theJournalUriSpecifiesTheJournalByTheNsdIdAndTheYear(Integer nsdCode, Integer journalYear) {
        Journal journal = extractJournal();
        String uriString = journal.getId().toString();
        assertThat(uriString, containsString(nsdCode.toString()));
        assertThat(uriString, containsString(journalYear.toString()));
    }

    @Then("the Nva Resource has a Reference object with doi equal to {string}")
    public void theNvaResourceHasAReferenceObjectWithDoiEqualTo(String expectedDoi) {
        Reference reference = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference();
        URI actualDoi = reference.getDoi();
        assertThat(actualDoi, is(equalTo(URI.create(expectedDoi))));
    }

    private CristinJournalPublicationJournal createCristinJournalPublicationJournal() {
        return CristinJournalPublicationJournal.builder()
            .withIssn(randomIssn())
            .withIssnOnline(randomIssn())
            .withJournalTitle(randomString())
            .build();
    }

    @Then("the Nva Resource, FeatureArticle, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceFeatureArticleHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        FeatureArticle featureArticle = (FeatureArticle) instance;
        String actualPagesBegin = featureArticle.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, FeatureArticle, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceFeatureArticleHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        FeatureArticle featureArticle = (FeatureArticle) instance;
        String actualPagesEnd = featureArticle.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, FeatureArticle, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceFeatureArticleHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        FeatureArticle featureArticle = (FeatureArticle) instance;
        String actualVolume = featureArticle.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Then("the Nva Resource, JournalLetter, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceJournalLetterHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalLetter journalLetter = (JournalLetter) instance;
        String actualPagesBegin = journalLetter.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, JournalLetter, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceJournalLetterHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalLetter journalLetter = (JournalLetter) instance;
        String actualPagesEnd = journalLetter.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, JournalLetter, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceJournalLetterHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalLetter journalLetter = (JournalLetter) instance;
        String actualVolume = journalLetter.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Then("the Nva Resource, JournalReview, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceJournalReviewHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalReview journalReview = (JournalReview) instance;
        String actualPagesBegin = journalReview.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, JournalReview, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceJournalReviewHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalReview journalReview = (JournalReview) instance;
        String actualPagesEnd = journalReview.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, JournalReview, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceJournalReviewHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalReview journalReview = (JournalReview) instance;
        String actualVolume = journalReview.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Then("the Nva Resource, JournalLeader, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceJournalLeaderHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalLeader journalLeader = (JournalLeader) instance;
        String actualPagesBegin = journalLeader.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, JournalLeader, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceJournalLeaderHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalLeader journalLeader = (JournalLeader) instance;
        String actualPagesEnd = journalLeader.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, JournalLeader, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceJournalLeaderHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalLeader journalLeader = (JournalLeader) instance;
        String actualVolume = journalLeader.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Then("the Nva Resource, JournalCorrigendum, has a PublicationContext with pagesBegin equal to {string}")
    public void theNvaResourceJournalCorrigendumHasAPublicationContextWithPagesBeginEqualTo(String expectedPagesBegin) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalCorrigendum journalCorrigendum = (JournalCorrigendum) instance;
        String actualPagesBegin = journalCorrigendum.getPages().getBegin();
        assertThat(actualPagesBegin, is(equalTo(expectedPagesBegin)));
    }

    @Then("the Nva Resource, JournalCorrigendum, has a PublicationContext with pagesEnd equal to {string}")
    public void theNvaResourceJournalCorrigendumHasAPublicationContextWithPagesEndEqualTo(String expectedPagesEnd) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalCorrigendum journalCorrigendum = (JournalCorrigendum) instance;
        String actualPagesEnd = journalCorrigendum.getPages().getEnd();
        assertThat(actualPagesEnd, is(equalTo(expectedPagesEnd)));
    }

    @Then("the Nva Resource, JournalCorrigendum, has a PublicationContext with volume equal to {string}")
    public void theNvaResourceJournalCorrigendumHasAPublicationContextWithVolumeEqualTo(String expectedVolume) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalCorrigendum journalCorrigendum = (JournalCorrigendum) instance;
        String actualVolume = journalCorrigendum.getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @And("the Journal Publication has a \"issue\" entry equal to {string}")
    public void theJournalPublicationHasAIssueEntryEqualTo(String issue) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .setIssue(issue);
    }

    @Then("the Nva Resource, FeatureArticle, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceFeatureArticleHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        FeatureArticle featureArticle = (FeatureArticle) instance;
        String actualIssue = featureArticle.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, Journal Article, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalArticleHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalArticle journalArticle = (JournalArticle) instance;
        String actualIssue = journalArticle.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, JournalCorrigendum, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalCorrigendumHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalCorrigendum journalCorrigendum = (JournalCorrigendum) instance;
        String actualIssue = journalCorrigendum.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, Journal Leader, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalLeaderHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalLeader journalLeader = (JournalLeader) instance;
        String actualIssue = journalLeader.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, Journal Letter, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalLetterHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalLetter journalLetter = (JournalLetter) instance;
        String actualIssue = journalLetter.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Then("the Nva Resource, Journal Review, has a PublicationContext with issue equal to {string}")
    public void theNvaResourceJournalReviewHasAPublicationContextWithIssueEqualTo(String expectedIssue) {
        PublicationInstance<? extends Pages> instance = scenarioContext
            .getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        JournalReview journalReview = (JournalReview) instance;
        String actualIssue = journalReview.getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }
}
