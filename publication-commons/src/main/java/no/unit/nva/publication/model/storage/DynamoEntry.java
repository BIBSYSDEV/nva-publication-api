package no.unit.nva.publication.model.storage;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;
import no.unit.nva.publication.storage.model.exceptions.EmptyValueMapException;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(Dao.class),
    @JsonSubTypes.Type(UniquenessEntry.class),
    @JsonSubTypes.Type(ImportCandidateDao.class)
})
public interface DynamoEntry {

    String CONTAINED_DATA_FIELD_NAME = "data";

    static <T> T parseAttributeValuesMap(Map<String, AttributeValue> valuesMap, Class<T> daoClass) {
        if (nonNull(valuesMap) && !valuesMap.isEmpty()) {
            if (hasByteArrayData(valuesMap)) {
                return DataCompressor.decompressDao(valuesMap, daoClass);
            } else {
                return parseDecompressedAttributeValue(valuesMap, daoClass);
            }

        } else {
            throw new EmptyValueMapException();
        }
    }

    @Deprecated // Delete after we have migrated all data to compressed
    private static <T> T parseDecompressedAttributeValue(Map<String, AttributeValue> valuesMap, Class<T> daoClass) {
        var document = EnhancedDocument.fromAttributeValueMap(valuesMap);
        return attempt(() -> dynamoDbObjectMapper.readValue(document.toJson(), daoClass)).orElseThrow();
    }

    private static boolean hasByteArrayData(Map<String, AttributeValue> valuesMap) {
        var dataAttribute = valuesMap.get(CONTAINED_DATA_FIELD_NAME);
        return nonNull(dataAttribute) && nonNull(dataAttribute.b());
    }

    @JsonIgnore
    SortableIdentifier getIdentifier();

    default Map<String, AttributeValue> toDynamoFormat() {
        var json = attempt(() -> dynamoDbObjectMapper.writeValueAsString(this)).orElseThrow();
        return EnhancedDocument.fromJson(json).toMap();
    }
}
