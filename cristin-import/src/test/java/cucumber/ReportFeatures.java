package cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class ReportFeatures {

    private final ScenarioContext scenarioContext;

    public ReportFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("the NVA Resource Report has a PublicationContext with publisher equal to {string}")
    public void theNvaResourceReportHasAPublicationContextWithPublisherEqualTo(String expectedPublisher) {
        PublicationContext context = scenarioContext.getNvaEntry()
                .getEntityDescription()
                .getReference()
                .getPublicationContext();
        Report reportContext = (Report) context;
        String actuallPublisher = reportContext.getPublisher();
        assertThat(actuallPublisher, is(equalTo(expectedPublisher)));
    }

    @Given("that the Cristin Result has an empty publisherName field")
    public void thatTheCristinResultHasAnEmptyPublisherNameField() {
        scenarioContext.getCristinEntry().getBookReport().setPublisherName(null);
    }
}
