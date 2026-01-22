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
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().nul(true).build();
        }
        if (value.getS() != null) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(value.getS()).build();
        }
        if (value.getN() != null) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n(value.getN()).build();
        }
        if (value.getB() != null) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                       .b(SdkBytes.fromByteBuffer(value.getB())).build();
        }
        if (value.getSS() != null && !value.getSS().isEmpty()) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().ss(value.getSS()).build();
        }
        if (value.getNS() != null && !value.getNS().isEmpty()) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().ns(value.getNS()).build();
        }
        if (value.getBS() != null && !value.getBS().isEmpty()) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                       .bs(value.getBS().stream().map(SdkBytes::fromByteBuffer).collect(Collectors.toList())).build();
        }
        if (value.getM() != null && !value.getM().isEmpty()) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                       .m(fromEventMapToDynamodbMap(value.getM())).build();
        }
        if (value.getL() != null && !value.getL().isEmpty()) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                       .l(value.getL().stream().map(DynamodbStreamRecordDaoMapper::toSdkAttributeValue)
                              .collect(Collectors.toList())).build();
        }
        if (value.getNULL() != null && value.getNULL()) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().nul(true).build();
        }
        if (value.getBOOL() != null) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                       .bool(value.getBOOL()).build();
        }
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().nul(true).build();
    }
}
