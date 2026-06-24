package no.unit.nva.publication.events.bodies;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Inverse of {@link StartMarkerSerializer}: reads a flat JSON object of string values back into a
 * DynamoDB start marker of {@code S} {@link AttributeValue}s.
 */
@SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
public class StartMarkerDeserializer extends JsonDeserializer<Map<String, AttributeValue>> {

  @Override
  public Map<String, AttributeValue> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode node = parser.readValueAsTree();
    if (isNull(node) || node.isNull()) {
      return null;
    }
    var startMarker = new LinkedHashMap<String, AttributeValue>();
    node.fields()
        .forEachRemaining(
            field ->
                startMarker.put(field.getKey(), AttributeValue.fromS(field.getValue().asText())));
    return startMarker;
  }
}
