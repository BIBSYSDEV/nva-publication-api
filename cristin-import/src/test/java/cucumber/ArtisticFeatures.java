package cucumber;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit;
import no.unit.nva.cristin.mapper.artisticproduction.CristinArtisticProduction;
import no.unit.nva.cristin.mapper.artisticproduction.CristinProduct;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.realization.OtherRelease;
import nva.commons.core.ioutils.IoUtils;

import java.nio.file.Path;

import static no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit.MINUTE;
import static no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtypeEnum.SHORT;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class ArtisticFeatures {

    private final ScenarioContext scenarioContext;

    public ArtisticFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @And("the Cristin result with both type_kunstneriskproduksjon and type_produkt present")
    public void validCristinResultWithBothTypeKunstneriskproduksjonAndTypeProduktPresent() {
        var artisticProduction = readArtisticProductionFromResource();
        var product = readProductFromResource();
        scenarioContext.getCristinEntry().setCristinArtisticProduction(artisticProduction);
        scenarioContext.getCristinEntry().setCristinProduct(product);
    }

    @Then("the NVA resources contains the data scraped from type_kunstneriskproduksjon")
    public void theNvaResourcesContainsTheDataScrapedFromTypeKunstneriskproduksjon() {
        var nvaResource = scenarioContext.getNvaEntry();
        var publicationInstance = nvaResource.getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance, is(instanceOf(MovingPicture.class)));
        var movingPicture = (MovingPicture) publicationInstance;
        assertThat(movingPicture.getSubtype().getType(), is(equalTo(SHORT)));
        assertThat(movingPicture.getOutputs(), hasSize(1));
        var output = movingPicture.getOutputs().get(0);
        assertThat(output, is(instanceOf(OtherRelease.class)));
        var otherRelease = (OtherRelease) output;
        var publisher = (UnconfirmedPublisher) otherRelease.getPublisher();
        assertThat(publisher.getName(), is(equalTo("Landbruksfilm")));
    }

    private String readResourceFile(Path path) {
        return IoUtils.stringFromResources(path);
    }

    private CristinProduct readProductFromResource() {
        var cristinProductString = readResourceFile(Path.of("type_produkt.json"));
        return attempt(
            () -> JsonUtils
                .dtoObjectMapper
                .readValue(cristinProductString, CristinProduct.class))
            .orElseThrow();
    }

    private CristinArtisticProduction readArtisticProductionFromResource() {
        var artisticProduction = readResourceFile(Path.of("type_kunstneriskproduksjon.json"));
        return attempt(
            () -> JsonUtils
                .dtoObjectMapper
                .readValue(artisticProduction, CristinArtisticProduction.class))
            .orElseThrow();
    }


    @And("the cristin result has a {string} present with duration equal to {string} minutes")
    public void theCristinResultHasAPresentWithDurationEqualToMinutes(String metadatafield, String minutes) {
        var timeUnit = new ArtisticProductionTimeUnit(MINUTE);
        if ("type_kunstneriskproduksjon".equals(metadatafield)) {
            var artisticProduction = readArtisticProductionFromResource();
            artisticProduction.setArtisticProductionTimeUnit(timeUnit);
            artisticProduction.setDuration(minutes);
            scenarioContext.getCristinEntry().setCristinArtisticProduction(artisticProduction);
            scenarioContext.getCristinEntry().setCristinProduct(null);
        }
        if ("type_produkt".equals(metadatafield)) {
            var product = readProductFromResource();
            product.setTimeUnit(timeUnit);
            product.setDuration(minutes);
            scenarioContext.getCristinEntry().setCristinProduct(product);
            scenarioContext.getCristinEntry().setCristinArtisticProduction(null);
        }

    }

    @Then("the Cristin Result contains a MovingPictureSubtypeEnum equal to {string}")
    public void theCristinResultContainsAMovingPictureSubtypeEnumEqualTo(String movingPictureSubtypeEnum) {
        var publicationInstance =
            scenarioContext.getNvaEntry().getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance, is(instanceOf(MovingPicture.class)));
        var movingPicture = (MovingPicture) publicationInstance;
        assertThat(movingPicture.getSubtype().getType().getType(), is(equalTo(movingPictureSubtypeEnum)));
    }

    @And("the cristin result lack the duration in both metadata fields")
    public void theCristinResultLackTheDurationInBothMetadataFields() {
        scenarioContext.getCristinEntry().getCristinArtisticProduction().setArtisticProductionTimeUnit(null);
        scenarioContext.getCristinEntry().getCristinProduct().setTimeUnit(null);
    }
}
