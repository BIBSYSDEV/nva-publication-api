package cucumber.features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import cucumber.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.net.URI;
import java.util.List;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

public class FileMergerFeatures {

    private final ScenarioContext scenarioContext;

    public FileMergerFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a brage publication with cristin identifier {string}")
    public void bragePublicationWithCristinIdentifier(String identifier) {
        scenarioContext.newBragePublication(identifier);
    }

    @And("a nva publication with cristin identifier {string}")
    public void nvaPublicationWithCristinIdentifier(String identifier) {
        scenarioContext.newNvaPublication(identifier);
    }

    @Given("a brage publication with handle {string}")
    public void bragePublicationWithHandle(String handle) {
        var bragePublication = scenarioContext.getBragePublication();
        bragePublication.setHandle(createHandleFromCandidate(handle));
    }

    @And("the nva publication has main handle {string}")
    public void nvaPublicationHasMainHandle(String handle) {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setHandle(createHandleFromCandidate(handle));
    }

    @And("the brage publication has a file with values:")
    public void bragePublicationHasAFileWithValues(PublishedFile publishedFile) {
        var bragePublication = scenarioContext.getBragePublication();
        bragePublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(publishedFile)));
    }

    @And("the nva publication has a file with values:")
    public void nvaPublicationHasAFileWithValues(PublishedFile publishedFile) {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(publishedFile)));
    }

    @Then("the merged nva publication has a file with values:")
    public void mergedNvaPublicationHasAFileWithValues(PublishedFile publishedFile) {
        var mergedPublication = scenarioContext.getMergedPublication();
        var associatedArtifacts = mergedPublication.getAssociatedArtifacts();
        assertThat(associatedArtifacts, hasSize(1));
        var associatedArtifact = associatedArtifacts.get(0);
        assertThat(associatedArtifact, is(instanceOf(PublishedFile.class)));
        var actualPublishedFile = (PublishedFile) associatedArtifact;
        assertThat(actualPublishedFile, is(samePropertyValuesAs(publishedFile)));
    }

    @And("the nva publication has a handle {string}")
    public void nvaPublicationHasAHandle(String handle) {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setHandle(createHandleFromCandidate(handle));
    }

    @And("the merged nva publication has a handle equal to {string}")
    public void mergedNvaPublicationHasAHandleEqualTo(String handle) {
        var mergedPublication = scenarioContext.getMergedPublication();
        assertThat(mergedPublication.getHandle(), is(equalTo(UriWrapper.fromUri(handle).getUri())));
    }

    @And("the nva publication has no associatedArtifacts")
    public void nvaPublicationHasNoAssociatedArtifacts() {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setAssociatedArtifacts(new AssociatedArtifactList());
    }

    @And("the brage publication has no associated artifacts")
    public void bragePublicationHasNoAssociatedArtifacts() {
        var bragePublication = scenarioContext.getBragePublication();
        bragePublication.setAssociatedArtifacts(new AssociatedArtifactList());
    }

    private static URI createHandleFromCandidate(String candidate) {
        return candidateIsNull(candidate) ? null : UriWrapper.fromUri(candidate).getUri();
    }

    private static boolean candidateIsNull(String candidate) {
        return StringUtils.isBlank(candidate) || "null".equalsIgnoreCase(candidate);
    }
}
