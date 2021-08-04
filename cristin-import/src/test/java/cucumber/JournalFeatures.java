package cucumber;

import static no.unit.nva.cristin.CristinDataGenerator.randomIssn;
import static no.unit.nva.cristin.CristinDataGenerator.randomString;
import static no.unit.nva.cristin.CristinDataGenerator.smallRandomNumber;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.net.URI;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Pages;

public class JournalFeatures {

    private final ScenarioContext scenarioContext;

    private static final String EMPTY_JOURNAL_TITLE = null;

    public JournalFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("that the Cristin Result has a non empty Journal Publication")
    public void thatTheCristinResultHasANonEmptyJournalPublication() {
        CristinJournalPublication journalPublication = CristinJournalPublication.builder()
            .withJournal(createCristinJournalPublicationJournal())
            .withPagesBegin("1")
            .withPagesEnd(String.valueOf(smallRandomNumber()))
            .withVolume(String.valueOf(smallRandomNumber()))
            .withDoi(String.valueOf(smallRandomNumber()))
            .build();
        scenarioContext.getCristinEntry().setJournalPublication(journalPublication);
    }

    @And("the Journal Publication has a \"journalName\" entry equal to {string}")
    public void theJournalPublicationHasApublisherNameEntryEqualTo(String publisherName) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .getJournal()
            .setJournalTitle(publisherName);
    }

    @And("the Journal Publication has a \"issn\" entry equal to {string}")
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
        Journal journal = (Journal) context;
        String actualIssn = journal.getPrintIssn();
        assertThat(actualIssn, is(equalTo(expectedIssn)));
    }

    @And("the Journal Publication has a \"issnOnline\" entry equal to {string}")
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
        Journal journal = (Journal) context;
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
        Journal journal = (Journal) context;
        String actualTitle = journal.getTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
    }

    @And("the Journal Publication has a \"pagesBegin\" entry equal to {string}")
    public void theJournalPublicationHasAPagesBeginEntryEqualTo(String pagesBegin) {
        scenarioContext.getCristinEntry()
                        .getJournalPublication()
                        .setPagesBegin(pagesBegin);
    }

    @And("the Journal Publication has a \"pagesEnd\" entry equal to {string}")
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

    @And("the Journal Publication has a \"volume\" entry equal to {string}")
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

    @And("the Journal Publication has a \"doi\" entry equal to {string}")
    public void theJournalPublicationHasADoiEntryEqualTo(String doi) {
        scenarioContext.getCristinEntry()
            .getJournalPublication()
            .setDoi(doi);
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
}
