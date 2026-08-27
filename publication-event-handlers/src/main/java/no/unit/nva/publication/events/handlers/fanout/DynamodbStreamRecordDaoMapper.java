package no.unit.nva.publication.events.handlers.fanout;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.transformers.v2.dynamodb.DynamodbAttributeValueTransformer;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.importcandidate.DatabaseEntryWithData;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;

public final class DynamodbStreamRecordDaoMapper {

  private DynamodbStreamRecordDaoMapper() {}

  public static Optional<Entity> toEntity(Map<String, AttributeValue> recordImage) {
    var attributeMap = DynamodbAttributeValueTransformer.toAttributeValueMapV2(recordImage);
    var dynamoEntry = DynamoEntry.parseAttributeValuesMap(attributeMap, DynamoEntry.class);
    return Optional.of(dynamoEntry)
        .filter(Dao.class::isInstance)
        .map(Dao.class::cast)
        .map(Dao::getData);
  }

  public static Optional<ImportCandidate> toImportCandidate(
      Map<String, AttributeValue> recordImage) {
    return Optional.ofNullable(DynamodbAttributeValueTransformer.toAttributeValueMapV2(recordImage))
        .map(
            attributeMap ->
                DatabaseEntryWithData.fromAttributeValuesMap(
                    attributeMap, ImportCandidateDao.class))
        .map(ImportCandidateDao::getData);
  }
}
