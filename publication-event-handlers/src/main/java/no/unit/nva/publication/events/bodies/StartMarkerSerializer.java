package no.unit.nva.publication.events.bodies;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Serializes a DynamoDB scan start marker (a {@code lastEvaluatedKey}, i.e. the table primary key)
 * to a flat JSON object of string values. The marker only ever contains string ({@code S})
 * attributes, so this avoids the need to (de)serialize the SDK v2 {@link AttributeValue} type,
 * which is not a Jackson bean.
 */
public class StartMarkerSerializer extends JsonSerializer<Map<String, AttributeValue>> {

  @Override
  public void serialize(
      Map<String, AttributeValue> startMarker,
      JsonGenerator generator,
      SerializerProvider serializers)
      throws IOException {
    if (isNull(startMarker)) {
      generator.writeNull();
    } else {
      generator.writeStartObject();
      for (var entry : startMarker.entrySet()) {
        generator.writeStringField(entry.getKey(), entry.getValue().s());
      }
      generator.writeEndObject();
    }
  }
}
