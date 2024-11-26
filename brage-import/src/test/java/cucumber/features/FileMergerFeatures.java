package cucumber.features;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import cucumber.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.pages.Range;
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
        bragePublication.brageRecord().setId(createHandleFromCandidate(handle));
        var additionalIdentifiers = new HashSet<>(bragePublication.publication().getAdditionalIdentifiers());
        additionalIdentifiers.add(new AdditionalIdentifier("handle", handle));
        bragePublication.publication().setAdditionalIdentifiers(additionalIdentifiers);
    }

    @And("the nva publication has main handle {string}")
    public void nvaPublicationHasMainHandle(String handle) {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setHandle(createHandleFromCandidate(handle));
    }

    @And("the brage publication has a file with values:")
    public void bragePublicationHasAFileWithValues(OpenFile openFile) {
        var bragePublication = scenarioContext.getBragePublication();
        bragePublication.publication().setAssociatedArtifacts(new AssociatedArtifactList(List.of(openFile)));
    }

    @And("the nva publication has a file with values:")
    public void nvaPublicationHasAFileWithValues(OpenFile openFile) {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(openFile)));
    }

    @Then("the merged nva publication has a file with values:")
    public void mergedNvaPublicationHasAFileWithValues(OpenFile openFile) {
        var mergedPublication = scenarioContext.getMergedPublication();
        var associatedArtifacts = mergedPublication.getAssociatedArtifacts();
        assertThat(associatedArtifacts, hasSize(1));
        var associatedArtifact = associatedArtifacts.getFirst();
        assertThat(associatedArtifact, is(instanceOf(OpenFile.class)));
        var actualopenFile = (OpenFile) associatedArtifact;
        assertThat(actualopenFile, is(samePropertyValuesAs(openFile)));
    }

    @And("the merged nva publication has a handle equal to {string} in additional identifiers")
    public void mergedNvaPublicationHasAHandleEqualTo(String handle) {
        var mergedPublication = scenarioContext.getMergedPublication();
        assertThat(mergedPublication.getAdditionalIdentifiers(), hasItem(new AdditionalIdentifier("handle", handle)));
    }

    @And("the nva publication has no associatedArtifacts")
    public void nvaPublicationHasNoAssociatedArtifacts() {
        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.setAssociatedArtifacts(new AssociatedArtifactList());
    }

    @And("the brage publication has no associated artifacts")
    public void bragePublicationHasNoAssociatedArtifacts() {
        var bragePublication = scenarioContext.getBragePublication();
        bragePublication.publication().setAssociatedArtifacts(new AssociatedArtifactList());
    }

    @And("the merged nva publication has a root level handle equal to {string}")
    public void theMergedNvaPublicationHasARootLevelHandleEqualTo(String handle) {
        var mergedPublication = scenarioContext.getMergedPublication();
        assertThat(mergedPublication.getHandle().toString(), equalTo(handle));
    }

    @And("the merged nva publication has a null handle")
    public void theMergedNvaPublicationHasANullHandle() {
        var mergedPublication = scenarioContext.getMergedPublication();
        assertThat(mergedPublication.getHandle(), is(nullValue()));
    }

    @And("both the publication instance is academic article")
    public void bothThePublicationInstanceIsAcademicArticle() throws InvalidIssnException {
        var academicArticle = new AcademicArticle(new Range(null, null), "1", "1", "1");
        var journal = new UnconfirmedJournal(randomString(), null, null);
        var bragePublication = scenarioContext.getBragePublication();
        bragePublication.publication().getEntityDescription().getReference().setPublicationInstance(academicArticle);
        bragePublication.publication().getEntityDescription().getReference().setPublicationContext(journal);

        var nvaPublication = scenarioContext.getNvaPublication();
        nvaPublication.getEntityDescription().getReference().setPublicationInstance(academicArticle);
        nvaPublication.getEntityDescription().getReference().setPublicationContext(journal);
    }

    private static URI createHandleFromCandidate(String candidate) {
        return candidateIsNull(candidate) ? null : UriWrapper.fromUri(candidate).getUri();
    }

    private static boolean candidateIsNull(String candidate) {
        return StringUtils.isBlank(candidate) || "null".equalsIgnoreCase(candidate);
    }
}
