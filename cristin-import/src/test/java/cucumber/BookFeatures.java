package cucumber;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.lambda.constants.MappingConstants;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinSubjectField;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.SingletonCollector;

public class BookFeatures {

    private final ScenarioContext scenarioContext;

    public BookFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("that the Cristin Result has a non empty Book Report")
    public void that_the_cristin_results_has_a_non_empty_type_book_report_field() {
        CristinBookOrReportMetadata bookReport = CristinDataGenerator.randomBookOrReportMetadata();
        scenarioContext.getCristinEntry().setBookOrReportMetadata(bookReport);
    }

    @Given("the Book Report has an ISBN version 10 with value {string}")
    public void theTypeBookReportFieldHasANonEmptyIsbnFieldWithValue(String cristinIsbn) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().setIsbn(cristinIsbn);
    }

    @Given("the Book Report has a \"total number of pages\" entry equal to {string}")
    public void theBookReportHasAEqualTo(String numberOfPages) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata()
            .setNumberOfPages(numberOfPages);
    }

    @Given("the Book Report has a \"publisher name\" entry equal to {string}")
    public void theBookReportHasAPublisherNameEntryEqualTo(String publisherName) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().setPublisherName(publisherName);
    }

    @Then("the NVA Resource has a PublicationContext with publisher with name equal to {string}")
    public void theNvaResourceHasAPublicationContextWithPublisherWithNameEqualTo(String expectedPublisherName) {
        var book = extractNvaBook();
        PublishingHouse expectedPublisher = new UnconfirmedPublisher(expectedPublisherName);
        assertThat(book.getPublisher(), is(equalTo(expectedPublisher)));
    }

    @Given("that the Book Report entry has an empty \"numberOfPages\" field")
    public void thatTheBookReportEntryHasAnEmptyNumberOfPagesField() {
        var bookReport = CristinDataGenerator.randomBookOrReportMetadata();
        bookReport.setNumberOfPages(null);
        scenarioContext.getCristinEntry().setBookOrReportMetadata(bookReport);
    }

    @Given("that the Book Report has a subjectField with the subjectFieldCode equal to {int}")
    public void thatTheBookReportHasASubjectFieldWithTheSubjectFieldCodeEqualTo(int subjectFieldCode) {
        scenarioContext.getCristinEntry()
            .getBookOrReportMetadata()
            .setSubjectField(CristinSubjectField
                                 .builder()
                                 .withSubjectFieldCode(subjectFieldCode)
                                 .build()
            );
    }

    @Given("the Cristin Result refers to a Series with NSD code {int}")
    public void theCristinResultRefersToSeriesWithNsdCode(Integer nsdCode) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().getBookSeries().setNsdCode(nsdCode);
    }

    @Given("that the Book Report has no subjectField")
    public void thatTheBookReportHasNoSubjectField() {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().setSubjectField(null);
    }

    @Given("the Cristin Result belongs to a Series")
    public void theCristinResultBelongsToASeries() {
        assertThat(this.scenarioContext.getCristinEntry().getBookOrReportMetadata(), is(notNullValue()));
    }

    @Given("the Series mentions a title {string}")
    public void theSeriesMentionsATitle(String seriesTitle) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().getBookSeries().setJournalTitle(seriesTitle);
    }

    @Given("the Series mentions an issn {string}")
    public void theSeriesMentionsAnIssn(String issn) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().getBookSeries().setIssn(issn);
    }

    @Given("the Series mentions online issn {string}")
    public void theSeriesMentionsOnlineIssn(String issn) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().getBookSeries().setIssnOnline(issn);
    }

    @Given("the Series mentions a volume {string}")
    public void theSeriesMentionsAVolume(String volume) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().setVolume(volume);
    }

    @Given("the Series mentions an issue {string}")
    public void theSeriesMentionsAnIssue(String issue) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().setIssue(issue);
    }

    @Given("the Series does not include an NSD code")
    public void theSeriesDoesNotIncludeAnNsdCode() {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().getBookSeries().setNsdCode(null);
    }

    @Given("the Cristin Result mentions a Publisher with NSD code {int}")
    public void theCristinResultMentionsAPublisherWithNsdCode(Integer publisherNsdCode) {
        this.scenarioContext.getCristinEntry()
            .getBookOrReportMetadata()
            .getCristinPublisher()
            .setNsdCode(publisherNsdCode);
    }

    @Given("the Cristin Result was reported in NVI the year {int}")
    public void theCristinResultWasReportedInNviTheYear(Integer yearReported) {
        this.scenarioContext.getCristinEntry().setYearReported(yearReported);
    }

    @Given("the Cristin Result mentions a Publisher with name {string} and without an NSD code")
    public void theCristinResultMentionsAPublisherWithNameAndWithoutAnNsdCode(String publisherName) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().getCristinPublisher()
            .setPublisherName(publisherName);
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().getCristinPublisher().setNsdCode(null);
    }

    @Given("the Cristin Result does not a primary Publisher entry")
    public void theCristinResultDoesNotAPrimaryPublisherEntry() {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().setCristinPublisher(null);
    }

    @Given("the Cristin Results has an alternative mention to a Publisher Name with value {string}")
    public void theCristinResultsHasAnAlternativeMentionToAPublisherNameWithValue(String publisherName) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().setPublisherName(publisherName);
    }

    @Then("the NVA Resource has a PublicationContext with an ISBN list containing the value {string}")
    public void theNvaResourceHasAPublicationContextWithAnIsbnListContainingTheValues(String expectedIsbn) {
        var bookContext = extractNvaBook();
        var singleIsbn = bookContext.getIsbnList().stream().collect(SingletonCollector.collect());
        assertThat(singleIsbn, is(equalTo(expectedIsbn)));
    }

    @Then("the NVA Resource has a PublicationContext with number of pages equal to {string}")
    public void theNvaResourceHasAPublicationContextWithNumberOfPagesEqualTo(String expectedNumberOfPages) {
        var context = scenarioContext.getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationInstance();
        var rawPages = context.getPages();
        var pages = rawPages instanceof MonographPages
                        ? ((MonographPages) rawPages).getPages()
                        : null;
        assertThat(pages, is(equalTo(expectedNumberOfPages)));
    }

    @Then("the NVA Resource has a npiSubjectHeading with value equal to {int}")
    public void theNvaResourceHasANpiSubjectHeadingWithValueEqualTo(int expectedSubjectFieldCode) {
        var actualSubjectFieldCode = scenarioContext.getNvaEntry()
                                         .getEntityDescription()
                                         .getNpiSubjectHeading();
        assertThat(actualSubjectFieldCode, is(equalTo(String.valueOf(expectedSubjectFieldCode))));
    }

    @Then("the NVA Resource contains a Publisher reference that is a URI pointing to the NVA NSD proxy")
    public void theNbaResourceContainsAPublisherReferenceThatIsAUriPointingToTheNvaNsdProxy() {
        var publisher = extractConfirmedPublisher();
        assertThat(publisher.getId().toString(), containsString(MappingConstants.NVA_API_DOMAIN));
    }

    @Then("the NVA Resource contains an Unconfirmed Series with title {string}, issn {string}, online issn {string} "
          + "and seriesNumber {string}")
    public void theNvaResourceContainsAnUnconfirmedSeriesWithTitleIssnOnlineIssnAndSeriesNumber(String title,
                                                                                                String issn,
                                                                                                String onlineIssn,
                                                                                                String seriesNumber) {
        var book = extractNvaBook();
        assertThat(book.getSeries().isConfirmed(), is(equalTo(false)));
        var unconfirmedSeries = (UnconfirmedSeries) book.getSeries();
        assertThat(unconfirmedSeries.getIssn(), is(equalTo(issn)));
        assertThat(unconfirmedSeries.getOnlineIssn(), is(equalTo(onlineIssn)));
        assertThat(unconfirmedSeries.getTitle(), is(equalTo(title)));
        assertThat(book.getSeriesNumber(), is(equalTo(seriesNumber)));
    }

    @Then("the NVA Resource mentions an Unconfirmed Publisher with name {string}")
    public void theNvaResourceMentionsAnUnconfirmedPublisherWithName(String publisherName) {
        var book = extractNvaBook();
        assertThat(book.getPublisher(), is(instanceOf(UnconfirmedPublisher.class)));
        var publisher = (UnconfirmedPublisher) book.getPublisher();
        assertThat(publisher.getName(), is(equalTo(publisherName)));
    }

    @Then("the Publisher URI contains the NSD code {string} and the publication year {int}")
    public void thePublisherUriContainsTheNsdCodeAndThePublicationYear(String pid, Integer publicationYear) {
        var publisherId = extractConfirmedPublisher().getId();
        assertThat(publisherId.getPath(), containsString(pid));
        assertThat(publisherId.getPath(), containsString(publicationYear.toString()));
    }

    @Then("the NVA Resource has a PublicationContext of type {string}")
    public void theNvaResourceHasAPublicationContextOfType(String publicationContextType) {
        var context = scenarioContext.getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        assertThat(context.getClass().getSimpleName(), is(equalTo(publicationContextType)));
    }

    @Then("the Cristin Result does not have an ISBN")
    public void theCristinResultDoesNotHaveAnIsbn() {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().setIsbn(null);
    }

    @Then("NVA Resource has a Publisher that cannot be verified through a URI")
    public void nvaResourceHasAPublisherThatCannotBeVerifiedThroughAUri() {
        var bookContext = extractNvaBook();
        var publisher = bookContext.getPublisher();
        assertThat(publisher, is(instanceOf(UnconfirmedPublisher.class)));
    }

    @Then("the NVA Resource has a Reference to a Series that is a URI pointing to the NVA NSD proxy")
    public void theNvaResourceHasAReferenceToAPublisherThatIsAUriPointingToTheNvaNsdProxy() {
        var seriesId = extractSeriesId();
        var expectedHost = URI.create(MappingConstants.NVA_API_DOMAIN).getHost();
        assertThat(seriesId.getHost(), is(equalTo(expectedHost)));
        assertThat(seriesId.getPath(), containsString(MappingConstants.NVA_CHANNEL_REGISTRY_V2));
        assertThat(seriesId.getPath(), containsString("serial-publication"));
    }

    @Then("the Series URI contains the NSD code {string} and the publication year {int}")
    public void theSeriesUriContainsTheNsdCodeAndThePublicationYear(String pid, Integer publicationYear) {
        var seriesId = extractSeriesId();
        assertThat(seriesId.getPath(), containsString(pid));
        assertThat(seriesId.getPath(), containsString(publicationYear.toString()));
    }

    @And("the Cristin Result has an valid ISBN littered with special characters {string}")
    public void theCristinResultHasAnValidIsbnLitteredWithSpecialCharactersString(String isbn) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().setIsbn(isbn);
    }

    @And("the Book Report has an ISBN field with value {string}")
    public void theBookReportHasAnIsbnFieldWithValue(String cristinIsbn) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().setIsbn(cristinIsbn);
    }

    @Then("the NVA Resource has a PublicationContext with an empty ISBN list")
    public void theNvaResourceHasAPublicationContextWithAnEmptyIsbnList() {
        var bookContext = extractNvaBook();
        var actualIsbnList = bookContext.getIsbnList();
        assertThat(actualIsbnList, hasSize(0));
    }

    @And("the Journal URI contains the PID code {string} and the publication year {int}")
    public void theJournalUriContainsThePidCodeAndThePublicationYear(String pid, Integer year) {
        var publisherId = extractConfirmedPublisher().getId();
        assertThat(publisherId.getPath(), containsString(pid));
        assertThat(publisherId.getPath(), containsString("serial-publication"));
        assertThat(publisherId.getPath(), containsString(year.toString()));
    }

    @And("the cristin Book Report has revision status equal to {string}")
    public void theCristinBookReportHasRevisionStatusEqualTo(String revision) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().setStatusRevision(revision);
    }

    @And("the book has series which has NSD code which does not exist in channel registry lookup file")
    public void the_book_has_nsd_code() {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().getBookSeries().setNsdCode(randomInteger());
    }

    @Given("the Book Publication has a reference to an NSD journal with identifier {int}")
    public void theJournalPublicationHasAReferenceToAnNsdJournalOrPublisherWithIdentifier(int nsdCode) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().getBookSeries().setNsdCode(nsdCode);
    }

    @And("the NVA Resource has a publication context Book with a revision equal to {string}")
    public void theNvaResourceHasAPublicationContextBookWithARevisionEqualTo(String revision) {
        var book = (Book) scenarioContext.getNvaEntry().getEntityDescription().getReference().getPublicationContext();
        assertThat(book.getRevision().getValue(), is(equalTo(revision)));
    }


    private Book extractNvaBook() {
        var context = this.scenarioContext.getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        return (Book) context;
    }

    private Publisher extractConfirmedPublisher() {
        var book = extractNvaBook();
        assertThat(book.getPublisher(), is(instanceOf(Publisher.class)));
        return (Publisher) book.getPublisher();
    }

    private URI extractSeriesId() {
        var bookSeries = Optional.of(
                this.scenarioContext.getNvaEntry()
                    .getEntityDescription()
                    .getReference()
                    .getPublicationContext())
                             .map(context -> (Book) context)
                             .map(Book::getSeries)
                             .filter(BookSeries::isConfirmed)
                             .map(series -> (Series) series)
                             .orElseThrow(() -> new IllegalStateException("BookSeries is not confirmed"));
        return bookSeries.getId();
    }
}
