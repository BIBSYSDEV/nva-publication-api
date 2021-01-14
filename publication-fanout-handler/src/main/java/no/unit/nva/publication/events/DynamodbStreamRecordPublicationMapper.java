package no.unit.nva.publication.events;

import static com.amazonaws.util.BinaryUtils.copyAllBytesFrom;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        Item item = toItem(attributeMap);
        Publication publication= objectMapper.readValue(item.toJSON(), Publication.class);
        return publication;
    }

    /*These methods are a copy of ItemUtils.toItem. The only difference is that instead of throwing an exception
     * when a field has empty values we do not throw an exception but we return null  */
    private static Item toItem(Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> attributeMap) {

        return Item.fromMap(toSimpleMapValue(attributeMap));
    }

    private static Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> fromEventMapToDynamodbMap(
            Map<String, AttributeValue> recordImage) throws JsonProcessingException {
        var jsonString = objectMapper.writeValueAsString(recordImage);
        var javaType = objectMapper.getTypeFactory().constructParametricType(Map.class, String.class,
                com.amazonaws.services.dynamodbv2.model.AttributeValue.class);
        return objectMapper.readValue(jsonString, javaType);
    }


    private static <T> Map<String, T> toSimpleMapValue(
        Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> values) {
        if (values == null) {
            return null;
        }

        Map<String, T> result = new LinkedHashMap<String, T>(values.size());
        for (Map.Entry<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> entry : values.entrySet()) {
            T t = toSimpleValue(entry.getValue());
            result.put(entry.getKey(), t);
        }
        return result;
    }

    private  static <T> T toSimpleValue(com.amazonaws.services.dynamodbv2.model.AttributeValue value) {
        if (value == null) {
            return null;
        }
        if (Boolean.TRUE.equals(value.getNULL())) {
            return null;
        } else if (Boolean.FALSE.equals(value.getNULL())) {
            throw new UnsupportedOperationException("False-NULL is not supported in DynamoDB");
        } else if (value.getBOOL() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) value.getBOOL();
            return t;
        } else if (value.getS() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) value.getS();
            return t;
        } else if (value.getN() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) new BigDecimal(value.getN());
            return t;
        } else if (value.getB() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) copyAllBytesFrom(value.getB());
            return t;
        } else if (value.getSS() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) new LinkedHashSet<String>(value.getSS());
            return t;
        } else if (value.getNS() != null) {
            Set<BigDecimal> set = new LinkedHashSet<BigDecimal>(value.getNS().size());
            for (String s : value.getNS()) {
                set.add(new BigDecimal(s));
            }
            @SuppressWarnings("unchecked")
            T t = (T) set;
            return t;
        } else if (value.getBS() != null) {
            Set<byte[]> set = new LinkedHashSet<byte[]>(value.getBS().size());
            for (ByteBuffer bb : value.getBS()) {
                set.add(copyAllBytesFrom(bb));
            }
            @SuppressWarnings("unchecked")
            T t = (T) set;
            return t;
        } else if (value.getL() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) toSimpleList(value.getL());
            return t;
        } else if (value.getM() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) toSimpleMapValue(value.getM());
            return t;
        } else {
            return null;
        }
    }

    private static List<Object> toSimpleList(List<com.amazonaws.services.dynamodbv2.model.AttributeValue> attrValues) {
        if (attrValues == null)
            return null;
        List<Object> result = new ArrayList<Object>(attrValues.size());
        for (com.amazonaws.services.dynamodbv2.model.AttributeValue attrValue : attrValues) {
            Object value = toSimpleValue(attrValue);
            result.add(value);
        }
        return result;
    }
}