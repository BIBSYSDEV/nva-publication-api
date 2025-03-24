package cucumber;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.net.URI;
import java.util.List;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinAssociatedUri;
import no.unit.nva.cristin.mapper.CristinMainCategory;
import no.unit.nva.cristin.mapper.CristinMediumTypeCode;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
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
    public void theNvaResourceHasAMediaContributionWithMedium(String mediumType) {
        var context = scenarioContext.getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        assertThat(context, is(instanceOf(MediaContribution.class)));
        var mediaContribution = (MediaContribution) context;
        assertThat(mediaContribution.getMedium().getType().getValue(), is(equalTo(mediumType)));
    }

    @And("the NVA resource has a MediaContribution with format {string}")
    public void theNvaResourceHasAMediaContributionWithFormat(String format) {
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

    @And("the cristin medium has a medium place name {string}")
    public void theCristinMediumHasAMediumPlaceName(String mediumPlaceName) {
        scenarioContext.getCristinEntry().getMediaContribution().setMediaPlaceName(mediumPlaceName);
    }

    @Then("the NVA publicationContext has a disseminationChannel equalTo {string}")
    public void theNvaPublicationContextHasADisseminationChannelEqualTo(
        String dissemintaionChannel) {
        var context = scenarioContext.getNvaEntry()
                          .getEntityDescription()
                          .getReference()
                          .getPublicationContext();
        assertThat(context, is(instanceOf(MediaContribution.class)));
        var mediaContribution = (MediaContribution) context;
        assertThat(mediaContribution.getDisseminationChannel(),
                   is(equalTo(dissemintaionChannel)));
    }

    @And("varbeid_url has url {string} of type {string}")
    public void varbeid_urlHasUrlOfType(String url, String type) {
        var associatedUri = CristinAssociatedUri.builder().withUrl(url).withUrlType(type).build();
        scenarioContext.getCristinEntry().setCristinAssociatedUris(List.of(associatedUri));
    }

    @Then("the NVA publication should contain associatedArtifacts containing associatedLink with {string}")
    public void theNVAPublicationShouldContainAssociatedArtifactsContainingAssociatedLinkWith(String string) {
        var associatedLink = scenarioContext.getNvaEntry().getAssociatedArtifacts()
                                 .stream().filter(AssociatedLink.class::isInstance)
                                 .map(AssociatedLink.class::cast)
                                 .findFirst()
                                 .orElseThrow();

        assertThat(associatedLink.id(), is(equalTo(URI.create(string))));
    }

    @Given("the Cristin Result has main category MEDIEBIDRAG")
    public void theCristinResultHasMainCategory() {
        this.scenarioContext.newCristinEntry(
            () -> CristinDataGenerator.randomObject(CristinSecondaryCategory.INTERVIEW.getValue()));
        assertThat(scenarioContext.getCristinEntry().getMainCategory(),
                   is(equalTo(CristinMainCategory.MEDIA_CONTRIBUTION)));
    }
}
