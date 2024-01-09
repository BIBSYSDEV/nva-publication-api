package cucumber;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import io.cucumber.java.en.Given;
import no.unit.nva.cristin.mapper.CristinLectureOrPosterMetaData;
import no.unit.nva.cristin.mapper.Event;

public class EventFeatures {

    private final ScenarioContext scenarioContext;

    public EventFeatures(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the Cristin Result has a non empty LectureOrPoster.")
    public void theCristinResultHasANonEmptyLectureOrPoster() {
        var event = Event.builder()
                        .withTitle(randomString())
                        .withAgent(randomString())
                        .withCountryCode(randomString())
                        .withPlace(randomString())
                        .withFrom("2023-11-28T00:00:00")
                        .withTo("2023-11-29T00:00:00")
                        .build();
        CristinLectureOrPosterMetaData cristinLectureOrPosterMetaData = CristinLectureOrPosterMetaData
                                                                            .builder()
                                                                            .withEvent(event)
                                                                            .build();
        scenarioContext.getCristinEntry().setLectureOrPosterMetaData(cristinLectureOrPosterMetaData);
    }
}
