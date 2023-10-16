package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.mapper.exhibition.CristinExhibition;
import no.unit.nva.cristin.mapper.exhibition.ExhibitionEvent;
import no.unit.nva.cristin.mapper.exhibition.MuseumEventCategory;
import no.unit.nva.model.UnconfirmedOrganization;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtype;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtypeEnum;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionBasic;
import no.unit.nva.model.time.Time;

public class ExhibitionProductionFeatures {

    private final ScenarioContext scenarioContext;

    public ExhibitionProductionFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @And("the cristin result has a museum category of {string}")
    public void theCristinResultHasAMuseumCategoryOf(String category) {
        var museumEvent = getExhibitionEvent();
        museumEvent.setMuseumEventCategory(MuseumEventCategory.builder().withEventCode(category).build());
    }

    @And("the cristin museum exhibits is permanent")
    public void theCristinMuseumExhibitsIsPermanent() {
        var exhibition = getCristinExhibition();
        exhibition.setStatusPermanent("J");
    }

    @Then("the NVA resource has an instance type exhibition production with a {string}")
    public void theNvaResourceHasAnInstanceTypeExhibitionProductionWithAExhibitionBasic(String type) {
        var exhibition = getExhibitionProduction();
        assertThat(exhibition.getSubtype(),
                   is(equalTo(new ExhibitionProductionSubtype(ExhibitionProductionSubtypeEnum.valueOf(type)))));
    }

    @And("the cristin exhibition event has a to date equal to null")
    public void theCristinExhibitionEventHasAToDateEqualToNull() {
        var exhibitionEvent = getExhibitionEvent();
        exhibitionEvent.setDateTo(null);
    }

    @And("the cristin exhibit has a event start of {string}")
    public void theCristinExhibitHasAEventStartOf(String date) {
        var exhibitionEvent = getExhibitionEvent();
        exhibitionEvent.setDateFrom(date);
    }

    @And("the cristin exhibit has a event end of {string}")
    public void theCristinExhibitHasAEventEndOf(String date) {
        var exhibitionEvent = getExhibitionEvent();
        exhibitionEvent.setDateTo(date);
    }

    @And("the exhibition manifestation has a period with date start equal to {string}")
    public void theExhibitionManifestationHasAPeriodWithDateStartEqualTo(String dateStart) {
        var basicManifestation = getExhibitionBasic();
        var period = basicManifestation.getDate();
        assertThat(period.getFrom(), is(equalTo(Time.convertToInstant(dateStart))));
    }

    @And("the exhibition manifestation has a period with date end equal to {string}")
    public void theExhibitionManifestationHasAPeriodWithDateEndEqualTo(String dateEnd) {
        var basicManifestation = getExhibitionBasic();
        var period = basicManifestation.getDate();
        assertThat(period.getTo(), is(equalTo(Time.convertToInstant(dateEnd))));
    }

    @And("the cristin exhibition event has an organizer equal to {string}")
    public void theCristinExhibitionEventHasAnOrganizerEqualTo(String organizer) {
        var exhibitionEvent = getExhibitionEvent();
        exhibitionEvent.setOrganizerName(organizer);
    }

    @And("the cristin exhibition event has an place description equal to {string}")
    public void theCristinExhibitionEventHasAnPlaceDescriptionEqualTo(String placeDescription) {
        var exhibitionEvent = getExhibitionEvent();
        exhibitionEvent.setPlaceDescription(placeDescription);
    }

    @And("the cristin exhibition event has a country code equal to {string}")
    public void theCristinExhibitionEventHasACountryCodeEqualTo(String countryCode) {
        var exhibitionEvent = getExhibitionEvent();
        exhibitionEvent.setCountryCode(countryCode);
    }

    @Then("the exhibition manifestation has an unconfirmed place with label equal to {string} and country equal to "
          + "{string}")
    public void theExhibitionManifestationHasAnUnconfirmedPlaceWithLabelEqualToAndCountryEqualTo(String label,
                                                                                                 String country) {
        var exhibitionBasic = getExhibitionBasic();
        assertThat(exhibitionBasic.getPlace(), is(equalTo(new UnconfirmedPlace(label, country))));
    }

    @And("the exhibition manifestation has a organization equal to {string}")
    public void theExhibitionManifestationHasAOrganizationEqualTo(String organization) {
        var exhibitionBasic = getExhibitionBasic();
        assertThat(exhibitionBasic.getOrganization(), is(equalTo(new UnconfirmedOrganization(organization))));
    }

    @And("the cristin exhibition event has a title {string}")
    public void theCristinExhibitionEventHasATitle(String exhibitionTitle) {
        var exhibitionEvent = getExhibitionEvent();
        exhibitionEvent.setTitleText(exhibitionTitle);
    }

    @Then("the NVA publication has a description containing {string}")
    public void theNvaPublicationHasADescriptionContaining(String description) {
        var actualDescription = scenarioContext.getNvaEntry().getEntityDescription().getDescription();
        assertThat(actualDescription, containsString(description));
    }

    @And("the NVA publication does not have a description containing {string}")
    public void theNvaPublicationDoesNotHaveADescriptionContaining(String description) {
        var actualDescription = scenarioContext.getNvaEntry().getEntityDescription().getDescription();
        assertThat(actualDescription, not(containsString(description)));
    }

    @And("the exhibition has a budget of {double} NOK")
    public void theExhibitionHasABudgetOfNOK(double budget) {
        var exhibition = getCristinExhibition();
        exhibition.setBudget(budget);
    }

    @And("the exhibition has an area of {double}")
    public void theExhibitionHasAnAreaOfM(double area) {
        var exhibition = getCristinExhibition();
        exhibition.setArea(area);
    }

    @And("the exhibition has {int} visitors")
    public void theExhibitionHasVisitors(int visitors) {
        var exhibition = getCristinExhibition();
        exhibition.setNumberOfVisitors(visitors);
    }

    @And("the exhibition has {int} number of total objects")
    public void theExhibitionHasNumberOfTotalObjects(int numberOfObjects) {
        var exhibition = getCristinExhibition();
        exhibition.setNumberOfObjectsInExhibit(numberOfObjects);
    }

    @And("the exhibition has {double} number of own objects")
    public void theExhibitionHasNumberOfOwnObjects(double percantageOwnedObjects) {
        var exhibition = getCristinExhibition();
        exhibition.setPercantageOfownedObjectsInExhibit(percantageOwnedObjects);
    }

    private ExhibitionEvent getExhibitionEvent() {
        return scenarioContext.getCristinEntry().getCristinExhibition().getExhibitionEvent();
    }

    private CristinExhibition getCristinExhibition() {
        return scenarioContext.getCristinEntry().getCristinExhibition();
    }

    private ExhibitionBasic getExhibitionBasic() {
        var exhibition = getExhibitionProduction();
        var manifestations = exhibition.getManifestations();
        assertThat(manifestations, hasSize(1));
        assertThat(manifestations.get(0), instanceOf(ExhibitionBasic.class));
        return (ExhibitionBasic) manifestations.get(0);
    }

    private ExhibitionProduction getExhibitionProduction() {
        var publicationInstance =
            scenarioContext.getNvaEntry().getEntityDescription().getReference().getPublicationInstance();
        assertThat(publicationInstance, instanceOf(ExhibitionProduction.class));
        return (ExhibitionProduction) publicationInstance;
    }
}
