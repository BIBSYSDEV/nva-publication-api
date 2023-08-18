package cucumber;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.cristin.mapper.artisticproduction.CristinArtisticProduction;
import no.unit.nva.cristin.mapper.artisticproduction.CristinProduct;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.realization.OtherRelease;
import nva.commons.core.ioutils.IoUtils;

import java.nio.file.Path;

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
    public void a_valid_cristin_result_with_both_type_kunstneriskproduksjon_and_type_produkt_present() {
        var artisticProduction = readArtisticProductionFromResource();
        var product = readProductFromResource();
        scenarioContext.getCristinEntry().setCristinArtisticProduction(artisticProduction);
        scenarioContext.getCristinEntry().setCristinProduct(product);
    }

    @Then("the NVA resources contains the data scraped from type_kunstneriskproduksjon")
    public void theNVAResourcesContainsTheDataScrapedFromType_kunstneriskproduksjon() {
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


}
