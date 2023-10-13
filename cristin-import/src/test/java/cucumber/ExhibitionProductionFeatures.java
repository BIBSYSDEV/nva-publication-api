package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.mapper.exhibition.MuseumEventCategory;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtype;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtypeEnum;

public class ExhibitionProductionFeatures {

    private final ScenarioContext scenarioContext;

    public ExhibitionProductionFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @And("the cristin result has a museum category of {string}")
    public void theCristinResultHasAMuseumCategoryOf(String category) {
        var museumEvent = scenarioContext.getCristinEntry().getCristinExhibition().getExhibitionEvent();
        museumEvent.setMuseumEventCategory(MuseumEventCategory.builder().withEventCode(category).build());
    }

    @And("the cristin museum exhibits is permanent")
    public void theCristinMuseumExhibitsIsPermanent() {
        var exhibition = scenarioContext.getCristinEntry().getCristinExhibition();
        exhibition.setStatusPermanent("J");
    }

    @Then("the NVA resource has an instance type exhibition production with a {string}")
    public void theNvaResourceHasAnInstanceTypeExhibitionProductionWithAExhibitionBasic(String type) {
        var publicationInstance =
            scenarioContext.getNvaEntry().getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance, instanceOf(ExhibitionProduction.class));
        var exhibition = (ExhibitionProduction) publicationInstance;
        assertThat(exhibition.getSubtype(),
                   is(equalTo(new ExhibitionProductionSubtype(ExhibitionProductionSubtypeEnum.valueOf(type)))));
    }

    @And("the cristin exhibition event has a to date equal to null")
    public void theCristinExhibitionEventHasAToDateEqualToNull() {
        var exhibitionEvent = scenarioContext.getCristinEntry().getCristinExhibition().getExhibitionEvent();
        exhibitionEvent.setDateTo(null);
    }

    @And("the cristin exhibit has a event start of {string}")
    public void theCristinExhibitHasAEventStartOf(String date) {
        var exhibitionEvent = scenarioContext.getCristinEntry().getCristinExhibition().getExhibitionEvent();
        exhibitionEvent.setDateFrom(date);
    }

    @And("the cristin exhibit has a event end of {string}")
    public void theCristinExhibitHasAEventEndOf(String date) {
        var exhibitionEvent = scenarioContext.getCristinEntry().getCristinExhibition().getExhibitionEvent();
        exhibitionEvent.setDateTo(date);
    }
}
