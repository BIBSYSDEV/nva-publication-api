package no.unit.nva.publication.events;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Publication;
import nva.commons.utils.JsonUtils;

import java.util.Map;

public final class DynamodbStreamRecordPublicationMapper {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    private DynamodbStreamRecordPublicationMapper() {

    }

    /**
     * Map a DynamodbStreamRecordImage to Publication.
     *
     * @param recordImage the record image (old or new)
     * @return publication
     * @throws JsonProcessingException JsonProcessingException
     */
    public static Publication toPublication(Map<String, AttributeValue> recordImage)
            throws JsonProcessingException {
        var attributeMap = fromEventMapToDynamodbMap(recordImage);
        Item item = ItemUtils.toItem(attributeMap);
        return objectMapper.readValue(item.toJSON(), Publication.class);
    }

    private static Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> fromEventMapToDynamodbMap(
            Map<String, AttributeValue> recordImage) throws JsonProcessingException {
        var jsonString = objectMapper.writeValueAsString(recordImage);
        var javaType = objectMapper.getTypeFactory().constructParametricType(Map.class, String.class,
                com.amazonaws.services.dynamodbv2.model.AttributeValue.class);
        return objectMapper.readValue(jsonString, javaType);
    }
}