package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;

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
        assertThat(context, is(instanceOf(Degree.class)));
    }
    
    @Given("the Cristin entry has a total number of pages equal to {string}")
    public void theCristinEntryHasATotalNumberOfPagesEqualTo(String numberOfPages) {
        scenarioContext.getCristinEntry()
            .getBookOrReportMetadata()
            .setNumberOfPages(numberOfPages);
    }
    
    @Then("the NVA DegreePhd has a PublicationContext with number of pages equal to {string}")
    public void theNvaDegreePhdHasAPublicationContextWithNumberOfPagesEqualTo(String expectedNumberOfPages) {
        PublicationInstance<?> instance = scenarioContext.getNvaEntry()
                                              .getEntityDescription()
                                              .getReference()
                                              .getPublicationInstance();
        DegreePhd degree = (DegreePhd) instance;
        assertThat(degree.getPages().getPages(), is(equalTo(expectedNumberOfPages)));
    }
    
    @Then("the NVA DegreeMaster has a PublicationContext with number of pages equal to {string}")
    public void theNvaDegreeMasterHasAPublicationContextWithNumberOfPagesEqualTo(String expectedNumberOfPages) {
        PublicationInstance<?> instance = scenarioContext.getNvaEntry()
                                              .getEntityDescription()
                                              .getReference()
                                              .getPublicationInstance();
        DegreeMaster degree = (DegreeMaster) instance;
        assertThat(degree.getPages().getPages(), is(equalTo(expectedNumberOfPages)));
    }

    @Then("the NVA DegreeLicentiate has a PublicationContext with number of pages equal to {string}")
    public void theNvaDegreeLicentiateHasAPublicationContextWithNumberOfPagesEqualTo(String expectedNumberOfPages) {
        PublicationInstance<?> instance = scenarioContext.getNvaEntry()
                                              .getEntityDescription()
                                              .getReference()
                                              .getPublicationInstance();
        DegreeLicentiate degree = (DegreeLicentiate) instance;
        assertThat(degree.getPages().getPages(), is(equalTo(expectedNumberOfPages)));
    }
}
