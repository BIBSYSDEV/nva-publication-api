package no.unit.nva.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import org.junit.jupiter.api.Test;

class EventMigrationTest {

    private static final String EVENT_TEMPLATE = """
        {
          "type": "Event",
          "name": "%s",
          "place": {
            "type": "UnconfirmedPlace",
            "label": "%s",
            "country": "GZSCXIUQTmZasj2zj8R"
          },
          "time": {
            "type": "Instant",
            "value": "2009-05-16T03:34:35.980Z"
          },
          "agent": {
            "type": "Organization",
            "id": "https://www.example.com/FBKFlbV9MDc4UWl1"
          },
          "product": "https://www.example.com/SW8hphJhrhfdOo6"
        }
        """;

    @Test
    void shouldMigrateEvent() {
        var event = EVENT_TEMPLATE.formatted("first", "second");
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(event, Event.class));
    }

    @Test
    void shouldMigrateEventNames() throws JsonProcessingException {
        var eventName = "first";
        var placeName = "second";
        var event = EVENT_TEMPLATE.formatted(eventName, placeName);
        var actual = JsonUtils.dtoObjectMapper.readValue(event, Event.class);
        assertThat(actual.getName(), is(equalTo(eventName)));
        assertThat(((UnconfirmedPlace) actual.getPlace()).name(), is(equalTo(placeName)));
    }
}
