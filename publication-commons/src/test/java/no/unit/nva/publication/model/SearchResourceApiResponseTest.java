package no.unit.nva.publication.model;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

public class SearchResourceApiResponseTest {

    @Test
    void shouldBeAbleToConvertSearchResourceApiRequestToJsonAndBackAgain() throws JsonProcessingException {
        var searchResponse = new SearchResourceApiResponse(1, List.of(randomPublication()));
        var json = searchResponse.toJsonString();
        var parsedSearchResponse = JsonUtils.dtoObjectMapper.readValue(json, SearchResourceApiResponse.class);
        assertThat(parsedSearchResponse, is(equalTo(searchResponse)));
    }
}
