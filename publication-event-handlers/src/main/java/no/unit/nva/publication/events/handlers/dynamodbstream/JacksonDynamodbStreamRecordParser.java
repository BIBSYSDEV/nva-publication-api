package no.unit.nva.publication.events.handlers.dynamodbstream;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toEntity;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;

public class JacksonDynamodbStreamRecordParser implements DynamodbStreamRecordParser {

    @Override
    public DataEntryUpdateEvent fromDynamoEventStreamRecordAsJson(String json) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        var root = attempt(() -> objectMapper.readTree(json)).orElseThrow();

        var action = root.get("eventName").asText();

        // Go into "dynamodb" → "NewImage"
        var oldImageNode = root.path("dynamodb").path("OldImage");
        var newImageNode = root.path("dynamodb").path("NewImage");

        var oldImage = isNull(oldImageNode) ? null : toEntity(parseAttributeMap(oldImageNode)).orElseThrow();
        var newImage = isNull(newImageNode) ? null : toEntity(parseAttributeMap(newImageNode)).orElseThrow();

        return new DataEntryUpdateEvent(action, oldImage, newImage);
    }

    private Map<String, AttributeValue> parseAttributeMap(JsonNode node) {
        var image = new HashMap<String, AttributeValue>();
        node.fieldNames().forEachRemaining(fieldName -> {
            var value = node.get(fieldName);

            var attributeValue = attempt(() -> parseAttributeValue(value)).orElseThrow();
            image.put(fieldName, attributeValue);
        });
        return image;
    }

    private static AttributeValue parseAttributeValue(JsonNode node) throws IOException {
        if (node.has("S")) {
            var value = new AttributeValue();
            value.setS(node.get("S").asText());
            return value;
        } else if (node.has("N")) {
            var value = new AttributeValue();
            value.setS(node.get("N").asText());
            return value;
        } else if (node.has("BOOL")) {
            var value = new AttributeValue();
            value.setBOOL(node.get("BOOL").asBoolean());
            return value;
        } else if (node.has("NULL")) {
            var value = new AttributeValue();
            value.setNULL(node.get("NULL").asBoolean());
            return value;
        } else if (node.has("M")) {
            var map = new HashMap<String, AttributeValue>();
            node.get("M").fieldNames().forEachRemaining(name -> {
                map.put(name, attempt(() -> parseAttributeValue(node.get(name))).orElseThrow());
            });
            var value = new AttributeValue();
            value.setM(map);
            return value;
        } else if (node.has("L")) {
            var list = new ArrayList<AttributeValue>();
            node.get("L").forEach(item -> list.add(attempt(() -> parseAttributeValue(item)).orElseThrow()));
            var value = new AttributeValue();
            value.setL(list);
            return value;
        } else if (node.has("B")) {
            var value = new AttributeValue();
            value.setB(ByteBuffer.wrap(node.get("B").asText().getBytes(StandardCharsets.UTF_8)));
            return value;
        }
        throw new IllegalArgumentException("Unsupported AttributeValue: " + node);
    }
}
