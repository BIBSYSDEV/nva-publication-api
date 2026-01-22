package no.unit.nva.publication.events.handlers.fanout;

import static java.util.Objects.isNull;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.importcandidate.DatabaseEntryWithData;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;
import software.amazon.awssdk.core.SdkBytes;

//TODO: rename class to DynamoJsonToInternalModelEventHandler
@SuppressWarnings({"PMD.ReturnEmptyCollectionRatherThanNull"})
public final class DynamodbStreamRecordDaoMapper {

    private DynamodbStreamRecordDaoMapper() {

    }

    /**
     * Map a DynamodbStreamRecordImage to Publication.
     *
     * @param recordImage the record image (old or new)
     * @return a Dao instance
     */
    public static Optional<Entity> toEntity(Map<String, AttributeValue> recordImage) {
        var attributeMap = fromEventMapToDynamodbMap(recordImage);
        var dynamoEntry = DynamoEntry.parseAttributeValuesMap(attributeMap, DynamoEntry.class);
        return Optional.of(dynamoEntry)
                   .filter(entry -> isDao(dynamoEntry))
                   .map(Dao.class::cast)
                   .map(Dao::getData)
                   .filter(DynamodbStreamRecordDaoMapper::isResourceUpdate);
    }

    public static Optional<ImportCandidate> toImportCandidate(Map<String, AttributeValue> recordImage) {
        return Optional.ofNullable(fromEventMapToDynamodbMap(recordImage))
                   .map(attributeMap -> DatabaseEntryWithData.fromAttributeValuesMap(attributeMap,
                                                                                     ImportCandidateDao.class))
                   .map(ImportCandidateDao::getData);
    }

    private static boolean isDao(DynamoEntry dynamoEntry) {
        return dynamoEntry instanceof Dao;
    }

    private static boolean isResourceUpdate(Object data) {
        return data instanceof Entity;
    }

    private static Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> fromEventMapToDynamodbMap(
            Map<String, AttributeValue> recordImage) {
        if (isNull(recordImage)) {
            return null;
        }
        return recordImage.entrySet().stream()
                   .collect(Collectors.toMap(Entry::getKey, e -> toSdkAttributeValue(e.getValue())));
    }

    private static software.amazon.awssdk.services.dynamodb.model.AttributeValue toSdkAttributeValue(
            AttributeValue value) {
        if (isNull(value)) {
            return buildNullValue();
        }
        return convertStringValue(value)
                   .or(() -> convertNumberValue(value))
                   .or(() -> convertBinaryValue(value))
                   .or(() -> convertStringSetValue(value))
                   .or(() -> convertNumberSetValue(value))
                   .or(() -> convertBinarySetValue(value))
                   .or(() -> convertMapValue(value))
                   .or(() -> convertListValue(value))
                   .or(() -> convertNullValue(value))
                   .or(() -> convertBoolValue(value))
                   .orElseGet(DynamodbStreamRecordDaoMapper::buildNullValue);
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertStringValue(
            AttributeValue value) {
        if (value.getS() != null) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .s(value.getS()).build());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertNumberValue(
            AttributeValue value) {
        if (value.getN() != null) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .n(value.getN()).build());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertBinaryValue(
            AttributeValue value) {
        if (value.getB() != null) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .b(SdkBytes.fromByteBuffer(value.getB())).build());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertStringSetValue(
            AttributeValue value) {
        if (value.getSS() != null && !value.getSS().isEmpty()) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .ss(value.getSS()).build());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertNumberSetValue(
            AttributeValue value) {
        if (value.getNS() != null && !value.getNS().isEmpty()) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .ns(value.getNS()).build());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertBinarySetValue(
            AttributeValue value) {
        if (value.getBS() != null && !value.getBS().isEmpty()) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .bs(value.getBS().stream().map(SdkBytes::fromByteBuffer)
                                           .collect(Collectors.toList())).build());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertMapValue(
            AttributeValue value) {
        if (value.getM() != null && !value.getM().isEmpty()) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .m(fromEventMapToDynamodbMap(value.getM())).build());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertListValue(
            AttributeValue value) {
        if (value.getL() != null && !value.getL().isEmpty()) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .l(value.getL().stream().map(DynamodbStreamRecordDaoMapper::toSdkAttributeValue)
                                          .collect(Collectors.toList())).build());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertNullValue(
            AttributeValue value) {
        if (value.getNULL() != null && value.getNULL()) {
            return Optional.of(buildNullValue());
        }
        return Optional.empty();
    }

    private static Optional<software.amazon.awssdk.services.dynamodb.model.AttributeValue> convertBoolValue(
            AttributeValue value) {
        if (value.getBOOL() != null) {
            return Optional.of(software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                   .bool(value.getBOOL()).build());
        }
        return Optional.empty();
    }

    private static software.amazon.awssdk.services.dynamodb.model.AttributeValue buildNullValue() {
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().nul(true).build();
    }
}
