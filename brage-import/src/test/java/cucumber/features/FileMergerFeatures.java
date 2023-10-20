package cucumber.features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import cucumber.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import nva.commons.core.paths.UriWrapper;

public class FileMergerFeatures {

    private final ScenarioContext scenarioContext;

    public FileMergerFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a brage publication with cristin identifier {string}")
    public void aBragePublicationWithCristinIdentifier(String identifier) {
        scenarioContext.newBragePublication(identifier);
    }

    @And("a nva publication with cristin identifier {string}")
    public void aNvaPublicationWithCristinIdentifier(String identifier) {
        scenarioContext.newNvaPublication(identifier);
    }

    @Given("a brage publication with handle {string}")
    public void aBragePublicationWithHandle(String handle) {
        var bragePublication = scenarioContext.getBragePublication();
        bragePublication.setHandle(UriWrapper.fromUri(handle).getUri());
    }

    @And("the nva publication has main handle {string}")
    public void theNvaPublicationHasMainHandle(String handle) {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setHandle(UriWrapper.fromUri(handle).getUri());
    }

    @And("the brage publication has a file with values:")
    public void theBragePublicationHasAFileWithValues(PublishedFile publishedFile) {
        var bragePublication = scenarioContext.getBragePublication();
        bragePublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(publishedFile)));
    }

    @And("the nva publication has a file with values:")
    public void theNvaPublicationHasAFileWithValues(PublishedFile publishedFile) {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(publishedFile)));
    }

    @Then("the merged nva publication has a file with values:")
    public void theMergedNvaPublicationHasAFileWithValues(PublishedFile publishedFile) {
        var mergedPublication = scenarioContext.getMergedPublication();
        var associatedArtifacts = mergedPublication.getAssociatedArtifacts();
        assertThat(associatedArtifacts, hasSize(1));
        var associatedArtifact = associatedArtifacts.get(0);
        assertThat(associatedArtifact, is(instanceOf(PublishedFile.class)));
        var actualPublishedFile = (PublishedFile) associatedArtifact;
        assertThat(actualPublishedFile, is(samePropertyValuesAs(publishedFile)));
    }

    @And("the nva publication has a handle {string}")
    public void theNvaPublicationHasAHandle(String handle) {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setHandle(UriWrapper.fromUri(handle).getUri());
    }
}
