package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

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
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.instancetypes.PeerReviewedMonograph;
import no.unit.nva.model.instancetypes.PublicationInstance;

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

    @Then("the NVA Resource has a PublicationContext with an ISBN list containing the value {string}")
    public void theNvaResourceHasAPublicationContextWithAnIsbnListContainingTheValues(String expectedIsbn) {
        PublicationContext publicationContext = scenarioContext.getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationContext();
        Book bookContext = (Book) publicationContext;
        String singleIsbn = bookContext.getIsbnList().stream().collect(SingletonCollector.collect());
        assertThat(singleIsbn, is(equalTo(expectedIsbn)));
    }

    @Given("the Book Report has a \"total number of pages\" entry equal to {string}")
    public void theBookReportHasAEqualTo(String numberOfPages) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata()
            .setNumberOfPages(numberOfPages);
    }

    @Then("the NVA Resource has a PublicationContext with number of pages equal to {string}")
    public void theNvaResourceHasAPublicationContextWithNumberOfPagesEqualTo(String expectedNumberOfPages) {
        PublicationInstance<?> context = scenarioContext.getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();
        PeerReviewedMonograph book = (PeerReviewedMonograph) context;
        assertThat(book.getPages().getPages(), is(equalTo(expectedNumberOfPages)));
    }

    @Given("the Book Report has a \"publisher name\" entry equal to {string}")
    public void theBookReportHasAPublisherNameEntryEqualTo(String publisherName) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().setPublisherName(publisherName);
    }

    @Then("the NVA Resource has a PublicationContext with publisher with name equal to {string}")
    public void theNvaResourceHasAPublicationContextWithPublisherWithNameEqualTo(String expectedPublisherName) {
        PublicationContext context = scenarioContext.getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationContext();
        Book book = (Book) context;
        PublishingHouse expectedPublisher = new UnconfirmedPublisher(expectedPublisherName);
        assertThat(book.getPublisher(), is(equalTo(expectedPublisher)));
    }

    @Given("that the Book Report entry has an empty \"numberOfPages\" field")
    public void thatTheBookReportEntryHasAnEmptyNumberOfPagesField() {
        CristinBookOrReportMetadata bookReport = CristinDataGenerator.randomBookOrReportMetadata();
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
    public void theCristinResultRefersToSeriesWithNSDCode(Integer nsdCode) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().getBookSeries().setNsdCode(nsdCode);
    }

    @Then("the NVA Resource has a npiSubjectHeading with value equal to {int}")
    public void theNvaResourceHasANpiSubjectHeadingWithValueEqualTo(int expectedSubjectFieldCode) {
        String actualSubjectFieldCode = scenarioContext.getNvaEntry()
            .getEntityDescription()
            .getNpiSubjectHeading();
        assertThat(actualSubjectFieldCode, is(equalTo(String.valueOf(expectedSubjectFieldCode))));
    }

    @Given("that the Book Report has no subjectField")
    public void thatTheBookReportHasNoSubjectField() {
        scenarioContext.getCristinEntry().getBookOrReportMetadata().setSubjectField(null);
    }

    @Then("the NVA Resource has a PublicationContext of type {string}")
    public void theNvaResourceHasAPublicationContextOfType(String publicationContextType) {
        PublicationContext context = scenarioContext.getNvaEntry()
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
        PublicationContext context = scenarioContext.getNvaEntry()
            .getEntityDescription()
            .getReference()
            .getPublicationContext();
        Book bookContext = (Book) context;
        PublishingHouse publisher = bookContext.getPublisher();
        assertThat(publisher, is(instanceOf(UnconfirmedPublisher.class)));
    }



    @Then("the NVA Resource has a Reference to a Publisher that is a URI pointing to the NVA NSD proxy")
    public void theNvaResourceHasAReferenceToAPublisherThatIsAUriPointingToTheNvaNsdProxy() {
        URI seriesId = extractSeriesId();
        String expectedHost = URI.create(MappingConstants.NVA_API_DOMAIN).getHost();
        assertThat(seriesId.getHost(), is(equalTo(expectedHost)));
        assertThat(seriesId.getPath(),containsString(MappingConstants.NSD_PROXY_PATH));



    }

    private URI extractSeriesId() {
        Series bookSeries = Optional.of(
                this.scenarioContext.getNvaEntry().getEntityDescription().getReference().getPublicationContext())
            .map(context -> (Book) context)
            .map(Book::getSeries)
            .filter(BookSeries::isConfirmed)
            .map(series -> (Series) series)
            .orElseThrow(() -> new IllegalStateException("BookSeries is not confirmed"));
        return bookSeries.getId();
    }

    @Then("the URI contains the NSD code {int} and the publication year {int}")
    public void theURIContainsTheNSDCodeAndThePublicationYear(Integer nsdCode, Integer publicationYear) {
        URI seriesId = extractSeriesId();
        assertThat(seriesId.getPath(),containsString(nsdCode.toString()));
        assertThat(seriesId.getPath(),containsString(publicationYear.toString()));
    }
}
