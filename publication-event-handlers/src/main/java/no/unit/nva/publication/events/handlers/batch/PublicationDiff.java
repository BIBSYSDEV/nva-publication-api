package no.unit.nva.publication.events.handlers.batch;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;

public final class PublicationDiff {

  private static final String ROOT_PATH = "";
  private static final String FIELD_PATH_FORMAT = "%s/%s";
  private static final String INDEX_PATH_FORMAT = "%s/%d";

  private PublicationDiff() {}

  public static JsonNode snapshot(Publication publication) {
    return JsonUtils.dtoObjectMapper.valueToTree(publication);
  }

  public static List<FieldChange> between(JsonNode before, JsonNode after) {
    var changes = new ArrayList<FieldChange>();
    collectChanges(ROOT_PATH, before, after, changes);
    return List.copyOf(changes);
  }

  private static void collectChanges(
      String path, JsonNode before, JsonNode after, List<FieldChange> changes) {
    if (before.equals(after)) {
      return;
    }
    if (before.isObject() && after.isObject()) {
      collectObjectChanges(path, before, after, changes);
    } else if (before.isArray() && after.isArray()) {
      collectArrayChanges(path, before, after, changes);
    } else {
      changes.add(new FieldChange(path, asText(before), asText(after)));
    }
  }

  private static void collectObjectChanges(
      String path, JsonNode before, JsonNode after, List<FieldChange> changes) {
    fieldNames(before, after)
        .forEach(
            fieldName ->
                collectChanges(
                    FIELD_PATH_FORMAT.formatted(path, fieldName),
                    before.path(fieldName),
                    after.path(fieldName),
                    changes));
  }

  private static void collectArrayChanges(
      String path, JsonNode before, JsonNode after, List<FieldChange> changes) {
    if (before.size() != after.size()) {
      changes.add(new FieldChange(path, before.toString(), after.toString()));
      return;
    }
    for (var index = 0; index < before.size(); index++) {
      collectChanges(
          INDEX_PATH_FORMAT.formatted(path, index), before.get(index), after.get(index), changes);
    }
  }

  private static Set<String> fieldNames(JsonNode before, JsonNode after) {
    var fieldNames = new LinkedHashSet<String>();
    before.fieldNames().forEachRemaining(fieldNames::add);
    after.fieldNames().forEachRemaining(fieldNames::add);
    return fieldNames;
  }

  private static String asText(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return null;
    }
    return node.isValueNode() ? node.asText() : node.toString();
  }
}
