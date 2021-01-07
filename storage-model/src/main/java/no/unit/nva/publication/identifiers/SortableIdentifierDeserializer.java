package no.unit.nva.publication.identifiers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class SortableIdentifierDeserializer extends JsonDeserializer<SortableIdentifier> {

    @Override
    public SortableIdentifier deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        String value=p.getValueAsString();
        return new SortableIdentifier(value);
    }
}
