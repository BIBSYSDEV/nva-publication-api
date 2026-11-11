package no.unit.nva.publication.scopusmapping;

import static java.util.stream.Collectors.toMap;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import nva.commons.core.ioutils.IoUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/** Reads items in DynamoDB JSON and writes them the way the PIA migration script does. */
final class ScopusMappingTestItems {

  static final String GOLDEN_FILE = "scopus-mapping-items.json";
  private static final int MAX_ITEMS_PER_BATCH_WRITE = 25;

  private ScopusMappingTestItems() {}

  static List<Map<String, AttributeValue>> readGoldenFile() {
    var json = IoUtils.stringFromResources(Path.of(GOLDEN_FILE));
    var itemNodes = attempt(() -> dtoObjectMapper.readTree(json)).orElseThrow();
    return StreamSupport.stream(itemNodes.spliterator(), false)
        .map(ScopusMappingTestItems::toItem)
        .toList();
  }

  static void writeWithBatchWriteItem(
      DynamoDbClient client, String tableName, List<Map<String, AttributeValue>> items) {
    assertThat(items)
        .as("items in one BatchWriteItem")
        .hasSizeLessThanOrEqualTo(MAX_ITEMS_PER_BATCH_WRITE);
    var writeRequests =
        items.stream()
            .map(
                item ->
                    WriteRequest.builder()
                        .putRequest(PutRequest.builder().item(item).build())
                        .build())
            .toList();
    var response =
        client.batchWriteItem(
            BatchWriteItemRequest.builder().requestItems(Map.of(tableName, writeRequests)).build());
    assertThat(response.unprocessedItems()).isEmpty();
  }

  private static Map<String, AttributeValue> toItem(JsonNode itemNode) {
    return itemNode.properties().stream()
        .collect(toMap(Map.Entry::getKey, attribute -> toAttributeValue(attribute.getValue())));
  }

  private static AttributeValue toAttributeValue(JsonNode typedValueNode) {
    assertThat(typedValueNode.size()).as("attribute type count of %s", typedValueNode).isOne();
    var typeAndValue = typedValueNode.properties().iterator().next();
    var valueNode = typeAndValue.getValue();
    return switch (typeAndValue.getKey()) {
      case "S" -> AttributeValue.fromS(requireText(valueNode));
      case "N" -> AttributeValue.fromN(requireText(valueNode));
      case "BOOL" -> AttributeValue.fromBool(requireBoolean(valueNode));
      case "L" ->
          AttributeValue.fromL(
              StreamSupport.stream(valueNode.spliterator(), false)
                  .map(ScopusMappingTestItems::toAttributeValue)
                  .toList());
      case "M" -> AttributeValue.fromM(toItem(valueNode));
      default ->
          throw new IllegalArgumentException(
              "Unsupported DynamoDB JSON type " + typeAndValue.getKey());
    };
  }

  private static String requireText(JsonNode valueNode) {
    assertThat(valueNode.isTextual()).as("%s is a JSON string", valueNode).isTrue();
    return valueNode.textValue();
  }

  private static boolean requireBoolean(JsonNode valueNode) {
    assertThat(valueNode.isBoolean()).as("%s is a JSON boolean", valueNode).isTrue();
    return valueNode.booleanValue();
  }
}
