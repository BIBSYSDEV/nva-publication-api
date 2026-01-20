package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.Collection;
import java.util.Map;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchFilterService {

  private static final Logger logger = LoggerFactory.getLogger(BatchFilterService.class);
  private static final Map<String, EntityFilterMatcher> FILTER_MATCHERS =
      Map.of(
          "Resource", new ResourceFilterMatcher(),
          "File", new FileFilterMatcher());

  public Collection<Map<String, AttributeValue>> applyFilter(
      Collection<Map<String, AttributeValue>> items, BatchFilter filter) {
    if (isNull(filter) || filter.isEmpty()) {
      return items;
    }
    return items.stream().filter(item -> matchesFilter(item, filter)).toList();
  }

  private boolean matchesFilter(Map<String, AttributeValue> item, BatchFilter filter) {
    try {
      var dao = DynamoEntry.parseAttributeValuesMap(item, Dao.class);
      var itemType = dao.dataType();

      var matcher = FILTER_MATCHERS.get(itemType);
      if (isNull(matcher)) {
        return false;
      }

      return matcher.matches(dao, filter);
    } catch (Exception e) {
      var partitionKey = item.getOrDefault("PK0", new AttributeValue("unknown")).getS();
      var sortKey = item.getOrDefault("SK0", new AttributeValue("unknown")).getS();
      logger.warn(
          "Failed to parse item for filtering, excluding item with PK0={}, SK0={}: {}",
          partitionKey,
          sortKey,
          e.getMessage());
      return false;
    }
  }
}
