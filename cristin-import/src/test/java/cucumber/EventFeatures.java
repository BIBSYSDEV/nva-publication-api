package cucumber;

import io.cucumber.java.en.Given;
import no.unit.nva.cristin.mapper.CristinLectureOrPosterMetaData;

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
}
