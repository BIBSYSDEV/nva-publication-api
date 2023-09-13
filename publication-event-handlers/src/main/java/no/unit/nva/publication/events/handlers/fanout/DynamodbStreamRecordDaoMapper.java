package no.unit.nva.publication.events.handlers.fanout;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;

//TODO: rename class to DynamoJsonToInternalModelEventHandler
public final class DynamodbStreamRecordDaoMapper {
    
    private DynamodbStreamRecordDaoMapper() {
    
    }
    
    /**
     * Map a DynamodbStreamRecordImage to Publication.
     *
     * @param recordImage the record image (old or new)
     * @return a Dao instance
     * @throws JsonProcessingException JsonProcessingException
     */
    public static Optional<Entity> toEntity(Map<String, AttributeValue> recordImage)
        throws JsonProcessingException {
        var attributeMap = fromEventMapToDynamodbMap(recordImage);
        var dynamoEntry = DynamoEntry.parseAttributeValuesMap(attributeMap, DynamoEntry.class);
        return Optional.of(dynamoEntry)
                   .filter(entry -> isDao(dynamoEntry))
                   .map(Dao.class::cast)
                   .map(Dao::getData)
                   .filter(DynamodbStreamRecordDaoMapper::isResourceUpdate);
    }

    private static boolean isDao(DynamoEntry dynamoEntry) {
        return dynamoEntry instanceof Dao;
    }
    
    private static boolean isResourceUpdate(Object data) {
        return data instanceof Entity;
    }

    private static Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> fromEventMapToDynamodbMap(
        Map<String, AttributeValue> recordImage) throws JsonProcessingException {
        var jsonString = objectMapper.writeValueAsString(recordImage);
        var javaType =
            objectMapper.getTypeFactory()
                .constructParametricType(Map.class,
                    String.class,
                    com.amazonaws.services.dynamodbv2.model.AttributeValue.class
                );
        return objectMapper.readValue(jsonString, javaType);
    }
}