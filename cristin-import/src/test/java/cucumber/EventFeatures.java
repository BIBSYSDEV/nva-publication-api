package cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.mapper.CristinLectureOrPosterMetaData;
import no.unit.nva.cristin.mapper.PresentationEvent;
import no.unit.nva.model.UnconfirmedOrganization;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.time.Period;

public class EventFeatures {

    private final ScenarioContext scenarioContext;

    public EventFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the Cristin Result has a non empty LectureOrPoster")
    public void theCristinResultHasANonEmptyLectureOrPoster() {
        CristinLectureOrPosterMetaData cristinLectureOrPosterMetaData = CristinLectureOrPosterMetaData.builder().build();
        scenarioContext.getCristinEntry().setLectureOrPosterMetaData(cristinLectureOrPosterMetaData);
    }

    @Given("the LectureOrPoster has an Event")
    public void theLectureOrPosterHasAEvent() {
        var event = PresentationEvent.builder().build();
        CristinLectureOrPosterMetaData cristinLectureOrPosterMetaData = CristinLectureOrPosterMetaData
                                                                            .builder()
                                                                            .withEvent(event)
                                                                            .build();
        scenarioContext.getCristinEntry().setLectureOrPosterMetaData(cristinLectureOrPosterMetaData);
    }

    @And("the PresentationEvent has a title {string}")
    public void theEventHasATitle(String value) {
        scenarioContext.getCristinEntry().getLectureOrPosterMetaData().getEvent().setTitle(value);
    }

    @And("the PresentationEvent has an Agent {string}")
    public void theEventHasAnAgent(String value) {
        scenarioContext.getCristinEntry().getLectureOrPosterMetaData().getEvent().setAgent(value);
    }

    @And("the PresentationEvent has a country code {string}")
    public void theEventHasACountryCode(String value) {
        scenarioContext.getCristinEntry().getLectureOrPosterMetaData().getEvent().setCountryCode(value);
    }

    @And("the PresentationEvent has a place {string}")
    public void theEventHasAPlace(String value) {
        scenarioContext.getCristinEntry().getLectureOrPosterMetaData().getEvent().setPlace(value);
    }

    @And("the PresentationEvent has a from date {string}")
    public void theEventHasAFromDate(String value) {
        scenarioContext.getCristinEntry().getLectureOrPosterMetaData().getEvent().setFrom(value);
    }

    @And("the PresentationEvent has a to date {string}")
    public void theEventHasAToDate(String value) {
        scenarioContext.getCristinEntry().getLectureOrPosterMetaData().getEvent().setTo(value);
    }

    @Then("the Event has a label {string}")
    public void theNvaEventHasALabel(String value) {
        var event = (Event) scenarioContext.getNvaEntry()
                                            .getEntityDescription()
                                            .getReference()
                                            .getPublicationContext();
        assertThat(event.getLabel(), is(equalTo(value)));
    }

    @Then("the Event has an agent {string}")
    public void theNvaEventHasAnAgent(String value) {
        var event = (Event) scenarioContext.getNvaEntry()
                                .getEntityDescription()
                                .getReference()
                                .getPublicationContext();
        assertThat(event.getAgent(), is(equalTo(new UnconfirmedOrganization(value))));
    }

    @Then("the Event has a place {string} and country code {string}")
    public void theNvaEventHasAPlaceWithLabelAndCountry(String place, String country) {
        var event = (Event) scenarioContext.getNvaEntry()
                                .getEntityDescription()
                                .getReference()
                                .getPublicationContext();
        assertThat(((UnconfirmedPlace) event.getPlace()), is(equalTo(new UnconfirmedPlace(place, country))));
    }

    @Then("the Event has a time Period with fromDate {string}")
    public void theNvaEventHasAPeriodWithFromDate(String value) {
        var event = (Event) scenarioContext.getNvaEntry()
                                .getEntityDescription()
                                .getReference()
                                .getPublicationContext();
        assertThat(((Period) event.getTime()).getFrom().toString(), is(equalTo(value)));
    }

    @Then("the Event has a time Period with toDate {string}")
    public void theNvaEventHasAPeriodWithToDate(String value) {
        var event = (Event) scenarioContext.getNvaEntry()
                                .getEntityDescription()
                                .getReference()
                                .getPublicationContext();
        assertThat(((Period) event.getTime()).getTo().toString(), is(equalTo(value)));
    }
}
