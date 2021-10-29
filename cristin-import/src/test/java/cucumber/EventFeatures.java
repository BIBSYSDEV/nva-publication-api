package cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import no.unit.nva.cristin.mapper.CristinEventMetaData;
import no.unit.nva.cristin.mapper.CristinLectureOrPosterMetaData;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.PublicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class EventFeatures {

    private final ScenarioContext scenarioContext;

    public EventFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the Cristin Result has a non empty LectureOrPoster.")
    public void theCristinResultHasANonEmptyLectureOrPoster() {
        CristinLectureOrPosterMetaData cristinLectureOrPosterMetaData = CristinLectureOrPosterMetaData
                .builder()
                .build();
        scenarioContext.getCristinEntry().setLectureOrPosterMetaData(cristinLectureOrPosterMetaData);
    }

    @Given("the Cristin Result has a event with the title {string}")
    public void theCristinResultHasAEventWithTheTitle(String title) {
        scenarioContext.getCristinEntry().getLectureOrPosterMetaData().getCristinEventMetaData().setTitle(title);
    }

    @Then("the NVA Resource has a event with the title {string}")
    public void theNvaResourceHasAEventWithTheTitle(String expectedTitle) {
        PublicationContext context = scenarioContext
                .getNvaEntry()
                .getEntityDescription()
                .getReference()
                .getPublicationContext();
        Event eventContext = (Event) context;
        String eventTitle = eventContext.getLabel();
        assertThat(eventTitle, is(equalTo(expectedTitle)));
    }

    @Given("the Cristin Result has an event")
    public void theCristinResultHasAnEvent() {
        CristinEventMetaData cristinEventMetaData = CristinEventMetaData.builder().build();
        CristinLectureOrPosterMetaData lectureOrPosterMetaData = CristinLectureOrPosterMetaData
                .builder()
                .withCristinEventMetaData(cristinEventMetaData)
                .build();
        scenarioContext.getCristinEntry().setLectureOrPosterMetaData(lectureOrPosterMetaData);
    }
}

