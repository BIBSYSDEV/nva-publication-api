package no.unit.nva.publication.identifiers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class SortableIdentifierSerializer extends JsonSerializer<SortableIdentifier> {

    public static final String NULL_AS_STRING = "null";
    public static final String SERIALIZATION_EXCEPTION_ERROR = "Could not serialize SortableIdentifier with value: ";

    @Override
    public void serialize(SortableIdentifier value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        try {
            if (Objects.nonNull(value)) {
                gen.writeString(value.toString());
            } else {
                gen.writeNull();
            }
        } catch (Exception e) {
            throw new RuntimeException(SERIALIZATION_EXCEPTION_ERROR + printIdentifierValue(value));
        }
    }

    private String printIdentifierValue(SortableIdentifier value) {
        return Optional.ofNullable(value).map(SortableIdentifier::toString).orElse(NULL_AS_STRING);
    }
}
