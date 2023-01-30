package no.unit.nva.publication.model.events;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import no.unit.nva.identifiers.SortableIdentifier;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

public class DeleteEntryEventTest {

    @Test
    void shouldSerializeFromJsonString() {
        var identifier = SortableIdentifier.next();
        var jsonString = new DeleteEntryEvent(DeleteEntryEvent.EVENT_TOPIC, identifier).toJsonString();
        var actualDeleteEntryEvent = DeleteEntryEvent.fromJson(jsonString);
        assertThat(actualDeleteEntryEvent, AllOf.allOf(hasProperty("topic",
                                                                   is(equalTo(DeleteEntryEvent.EVENT_TOPIC))),
                                                       hasProperty("identifier", is(equalTo(identifier)))));
    }
}
