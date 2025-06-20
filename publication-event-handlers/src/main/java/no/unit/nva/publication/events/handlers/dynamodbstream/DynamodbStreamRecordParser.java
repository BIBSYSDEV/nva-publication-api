package no.unit.nva.publication.events.handlers.dynamodbstream;

import java.io.IOException;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;

public interface DynamodbStreamRecordParser {

    DataEntryUpdateEvent fromDynamoEventStreamRecordAsJson(String json) throws IOException;
}
