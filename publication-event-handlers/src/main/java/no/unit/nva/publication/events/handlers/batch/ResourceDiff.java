package no.unit.nva.publication.events.handlers.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.Resource;

public final class ResourceDiff {

  private static final String ROOT_PATH = "";
  private static final String FIELD_PATH_FORMAT = "%s/%s";
  private static final String INDEX_PATH_FORMAT = "%s/%d";

  private ResourceDiff() {}

  public static JsonNode snapshot(Resource resource) {
    return JsonUtils.dtoObjectMapper.valueToTree(resource);
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

  /**
   * Elements present on both sides are paired up regardless of position, so reordering alone is not
   * reported. What is left over is diffed pairwise, which turns a replaced element into a change on
   * its own fields rather than a change on every element after it. A changed or removed element is
   * reported at its index in the old array, an added one at its index in the new array.
   */
  private static void collectArrayChanges(
      String path, JsonNode before, JsonNode after, List<FieldChange> changes) {
    var added = indexedElementsOf(after);
    var removed = elementsWithoutCounterpart(before, added);

    for (var position = 0; position < Math.max(removed.size(), added.size()); position++) {
      collectChanges(
          INDEX_PATH_FORMAT.formatted(path, reportedIndex(removed, added, position)),
          nodeAt(removed, position),
          nodeAt(added, position),
          changes);
    }
  }

  private static List<IndexedElement> elementsWithoutCounterpart(
      JsonNode before, List<IndexedElement> added) {
    var removed = new ArrayList<IndexedElement>();
    for (var index = 0; index < before.size(); index++) {
      var element = before.get(index);
      if (!removeFirstMatching(added, element)) {
        removed.add(new IndexedElement(index, element));
      }
    }
    return removed;
  }

  private static boolean removeFirstMatching(List<IndexedElement> added, JsonNode element) {
    return added.stream()
        .filter(candidate -> candidate.node().equals(element))
        .findFirst()
        .map(added::remove)
        .orElse(false);
  }

  private static int reportedIndex(
      List<IndexedElement> removed, List<IndexedElement> added, int position) {
    return position < removed.size() ? removed.get(position).index() : added.get(position).index();
  }

  private static JsonNode nodeAt(List<IndexedElement> elements, int position) {
    return position < elements.size() ? elements.get(position).node() : MissingNode.getInstance();
  }

  private static List<IndexedElement> indexedElementsOf(JsonNode array) {
    var elements = new ArrayList<IndexedElement>();
    for (var index = 0; index < array.size(); index++) {
      elements.add(new IndexedElement(index, array.get(index)));
    }
    return elements;
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

  private record IndexedElement(int index, JsonNode node) {}
}
