package cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PeerReviewedMonograph;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreePhd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;


public class DegreeFeatures {

    private final ScenarioContext scenarioContext;

    public DegreeFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("the NVA Resource has a PublicationContext of type Degree")
    public void theNvaResourceHasAPublicationContextOfTypeDegree() {
        PublicationContext context = scenarioContext.getNvaEntry()
                .getEntityDescription()
                .getReference()
                .getPublicationContext();
        Book book = (Book) context;
        assertThat(context, is(instanceOf(Degree.class)));
    }

    @Given("the Cristin entry has a total number of pages equal to {string}")
    public void theCristinEntryHasATotalNumberOfPagesEqualTo(String numberOfPages) {
        scenarioContext.getCristinEntry().getBookOrReportMetadata()
                .setNumberOfPages(numberOfPages);
    }

    @Then("the NVA Degree has a PublicationContext with number of pages equal to {string}")
    public void theNvaDegreeHasAPublicationContextWithNumberOfPagesEqualTo(String expectedNumberOfPages) {
        PublicationInstance<?> instance = scenarioContext.getNvaEntry()
                .getEntityDescription()
                .getReference()
                .getPublicationInstance();
        DegreePhd degree = (DegreePhd) instance;
        assertThat(degree.getPages().getPages(), is(equalTo(expectedNumberOfPages)));
    }
}
