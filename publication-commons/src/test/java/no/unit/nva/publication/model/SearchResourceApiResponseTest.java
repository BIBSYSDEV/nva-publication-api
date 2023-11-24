package no.unit.nva.publication.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

public class SearchResourceApiResponseTest {

    @Test
    void shouldBeAbleToConvertSearchResourceApiRequestToJsonAndBackAgain() throws JsonProcessingException {
        var resourceWithId = new ResourceWithId(
            UriWrapper.fromUri("https://api.test.nva.aws.unit.no/publication/" + SortableIdentifier.next())
                .getUri());
        var searchResponse = new SearchResourceApiResponse(1, List.of(resourceWithId));
        var json = searchResponse.toJsonString();
        var parsedSearchResponse = JsonUtils.dtoObjectMapper.readValue(json, SearchResourceApiResponse.class);
        assertThat(parsedSearchResponse, is(equalTo(searchResponse)));
    }

    @Test
    void shouldReturnSortableIdentifierWhenCallingGetIdentifierMethod() {
        var identifier = SortableIdentifier.next();
        var resourceWithId = new ResourceWithId(
            UriWrapper.fromUri("https://api.test.nva.aws.unit.no/publication/" + identifier)
                .getUri());
        assertThat(resourceWithId.getIdentifier(), is(equalTo(identifier)));
    }
}
