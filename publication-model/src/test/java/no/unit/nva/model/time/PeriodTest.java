package no.unit.nva.model.time;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

public class PeriodTest {

    @Test
    void shouldParseEmptyToDate() throws JsonProcessingException {
        var periodString = "{\"from\": \"2023-10-09T22:00:00.000Z\",\"to\": \"\",\"type\": \"Period\"}";
        var period = JsonUtils.dtoObjectMapper.readValue(periodString, Period.class);
        assertThat(period.getTo(), is(nullValue()));
        assertThat(period.getFrom(), is(notNullValue()));
    }

}
