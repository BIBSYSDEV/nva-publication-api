package no.unit.nva.publication.events.handlers.fanout;

import static java.util.Objects.nonNull;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.importcandidate.DatabaseEntryWithData;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;
import software.amazon.awssdk.core.SdkBytes;

// TODO: rename class to DynamoJsonToInternalModelEventHandler
@SuppressWarnings({"PMD.ReturnEmptyCollectionRatherThanNull"})
public final class DynamodbStreamRecordDaoMapper {

  private DynamodbStreamRecordDaoMapper() {}

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

  public static Optional<ImportCandidate> toImportCandidate(Map<String, AttributeValue> recordImage)
      throws JsonProcessingException {
    return Optional.ofNullable(fromEventMapToDynamodbMap(recordImage))
        .map(
            attributeMap ->
                DatabaseEntryWithData.fromAttributeValuesMap(
                    attributeMap, ImportCandidateDao.class))
        .map(ImportCandidateDao::getData);
  }

  private static boolean isDao(DynamoEntry dynamoEntry) {
    return dynamoEntry instanceof Dao;
  }

  private static boolean isResourceUpdate(Object data) {
    return data instanceof Entity;
  }

  private static Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>
      fromEventMapToDynamodbMap(Map<String, AttributeValue> recordImage) {
    return recordImage.entrySet().stream()
        .collect(
            Collectors.toMap(Map.Entry::getKey, entry -> toSdkAttributeValue(entry.getValue())));
  }

  private static software.amazon.awssdk.services.dynamodb.model.AttributeValue toSdkAttributeValue(
      AttributeValue value) {
    var builder = software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder();
    if (nonNull(value.getS())) {
      builder.s(value.getS());
    } else if (nonNull(value.getN())) {
      builder.n(value.getN());
    } else if (nonNull(value.getBOOL())) {
      builder.bool(value.getBOOL());
    } else if (nonNull(value.getNULL())) {
      builder.nul(value.getNULL());
    } else if (nonNull(value.getM())) {
      builder.m(fromEventMapToDynamodbMap(value.getM()));
    } else if (nonNull(value.getL())) {
      builder.l(
          value.getL().stream().map(DynamodbStreamRecordDaoMapper::toSdkAttributeValue).toList());
    } else if (nonNull(value.getSS())) {
      builder.ss(value.getSS());
    } else if (nonNull(value.getNS())) {
      builder.ns(value.getNS());
    } else if (nonNull(value.getB())) {
      builder.b(SdkBytes.fromByteBuffer(value.getB()));
    } else if (nonNull(value.getBS())) {
      builder.bs(value.getBS().stream().map(SdkBytes::fromByteBuffer).toList());
    }
    return builder.build();
  }
}
