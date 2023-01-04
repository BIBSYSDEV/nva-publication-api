package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.PublicationContext;

public class MediaContributionFeatures {

    private final ScenarioContext scenarioContext;

    public MediaContributionFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("the NVA Resource has a PublicationContext of type MediaContribution")
    public void theNvaResourceHasAPublicationContextOfTypeMediaContribution() {
        PublicationContext context = scenarioContext.getNvaEntry()
                                         .getEntityDescription()
                                         .getReference()
                                         .getPublicationContext();
        assertThat(context, is(instanceOf(MediaContribution.class)));
    }

    @And("the cristin result has mediaContribution with mediumType equal to {string}")
    public void theCristinResultHasMediaContributionWithMediumTypeEqualTo(String mediumType) {
        scenarioContext
            .getCristinEntry()
            .getMediaContribution()
            .getCristinMediumType()
            .setMediumTypeCode(mediumType);
    }

    @Then("the NVA resource has a MediaContribution with medium {string}")
    public void theNVAResourceHasAMediaContributionWithMedium(String mediumType) {
        var context = scenarioContext.getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        assertThat(context, is(instanceOf(MediaContribution.class)));
        var mediaContribution = (MediaContribution) context;
        assertThat(mediaContribution.getMedium().getType().getValue(), is(equalTo(mediumType)));
    }
}
