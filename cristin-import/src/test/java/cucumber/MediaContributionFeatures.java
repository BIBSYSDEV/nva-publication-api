package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.mapper.CristinMediumTypeCode;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.media.MediaFormat;

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
            .setMediumTypeCode(CristinMediumTypeCode.fromValue(mediumType));
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

    @And("the NVA resource has a MediaContribution with format {string}")
    public void theNVAResourceHasAMediaContributionWithFormat(String format) {
        var nullString = "NULL";
        var context = scenarioContext.getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        assertThat(context, is(instanceOf(MediaContribution.class)));
        var mediaContribution = (MediaContribution) context;
        var expectedFormat = nullString.equals(format) ? null : MediaFormat.valueOf(format);
        assertThat(mediaContribution.getFormat(), is(equalTo(expectedFormat)));
    }
}
