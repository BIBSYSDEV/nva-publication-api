package cucumber;

import static no.unit.nva.cristin.CristinDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinBookReport;
import no.unit.nva.cristin.mapper.CristinSubjectField;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import nva.commons.core.SingletonCollector;

import java.util.List;

public class BookFeatures {

    public static final int SINGLE_BOOK_REPORT = 0;
    private final ScenarioContext scenarioContext;

    public BookFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("that the Cristin Result has a non empty Book Report")
    public void that_the_cristin_results_has_a_non_empty_type_book_report_field() {
        CristinBookReport bookReport = CristinDataGenerator.randomBookReport();
        scenarioContext.getCristinEntry().setBookReport(bookReport);
    }

    @Given("the Book Report has an ISBN version 10 with value {string}")
    public void theTypeBookReportFieldHasANonEmptyIsbnFieldWithValue(String cristinIsbn) {
        scenarioContext.getCristinEntry().getBookReport().setIsbn(cristinIsbn);
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
        scenarioContext.getCristinEntry().getBookReport()
            .setNumberOfPages(numberOfPages);
    }

    @Then("the NVA Resource has a PublicationContext with number of pages equal to {string}")
    public void theNvaResourceHasAPublicationContextWithNumberOfPagesEqualTo(String expectedNumberOfPages) {
        PublicationInstance<?> context = scenarioContext.getNvaEntry()
                                             .getEntityDescription()
                                             .getReference()
                                             .getPublicationInstance();
        BookMonograph book = (BookMonograph) context;
        assertThat(book.getPages().getPages(), is(equalTo(expectedNumberOfPages)));
    }

    @Given("the Book Report has a \"publisher name\" entry equal to {string}")
    public void theBookReportHasAPublisherNameEntryEqualTo(String publisherName) {
        scenarioContext.getCristinEntry().getBookReport().setPublisherName(publisherName);
    }

    @Then("the NVA Resource has a PublicationContext with publisher equal to {string}")
    public void theNvaResourceHasAPublicationContextWithPublisherEqualTo(String expectedPublisherName) {
        PublicationContext context = scenarioContext.getNvaEntry()
                                         .getEntityDescription()
                                         .getReference()
                                         .getPublicationContext();
        Book book = (Book) context;
        assertThat(book.getPublisher(), is(equalTo(expectedPublisherName)));
    }

    @Given("that the Book Report entry has an empty \"numberOfPages\" field")
    public void thatTheBookReportEntryHasAnEmptyNumberOfPagesField() {
        CristinBookReport bookReport = CristinBookReport.builder()
                .withNumberOfPages(null)
                .withPublisherName(randomString())
                .withIsbn(CristinDataGenerator.randomIsbn13())
                .build();
        scenarioContext.getCristinEntry().setBookReport(bookReport);
    }

    @And("that the Book Report has a subjectField with the subjectFieldCode equal to {int}")
    public void thatTheBookReportHasASubjectFieldWithTheSubjectFieldCodeEqualTo(int subjectFieldCode) {
        scenarioContext.getCristinEntry()
                        .getBookReport()
                        .setSubjectField(CristinSubjectField
                                        .builder()
                                        .withSubjectFieldCode(subjectFieldCode)
                                        .build()
            );
    }

    @Then("the NVA Resource has a npiSubjectHeading with value equal to {int}")
    public void theNvaResourceHasANpiSubjectHeadingWithValueEqualTo(int expectedSubjectFieldCode) {
        String actuallSubjectFieldCode = scenarioContext.getNvaEntry()
                                                        .getEntityDescription()
                                                        .getNpiSubjectHeading();
        assertThat(actuallSubjectFieldCode, is(equalTo(String.valueOf(expectedSubjectFieldCode))));
    }

    @And("that the Book Report has no subjectField")
    public void thatTheBookReportHasNoSubjectField() {
        scenarioContext.getCristinEntry().getBookReport().setSubjectField(null);
    }
}
