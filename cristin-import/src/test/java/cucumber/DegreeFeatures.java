package cucumber;

import io.cucumber.java.en.Then;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.PublicationContext;

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
}
