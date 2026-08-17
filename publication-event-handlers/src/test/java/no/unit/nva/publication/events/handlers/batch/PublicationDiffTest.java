package no.unit.nva.publication.events.handlers.batch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class PublicationDiffTest {

  private static final String FIRST_ELEMENT_PATH = "/items/0";
  private static final String FIRST_ELEMENT_NAME_PATH = "/items/0/name";

  @Test
  void shouldReportNoChangesWhenArrayElementsAreOnlyReordered() {
    var before = arrayOf("[{\"id\":\"a\"},{\"id\":\"b\"},{\"id\":\"c\"}]");
    var after = arrayOf("[{\"id\":\"c\"},{\"id\":\"a\"},{\"id\":\"b\"}]");

    assertThat(PublicationDiff.between(before, after), empty());
  }

  @Test
  void shouldReportChangedElementOnlyWhenOtherElementsAreReordered() {
    var before = arrayOf("[{\"id\":\"a\",\"name\":\"old\"},{\"id\":\"b\"},{\"id\":\"c\"}]");
    var after = arrayOf("[{\"id\":\"c\"},{\"id\":\"b\"},{\"id\":\"a\",\"name\":\"new\"}]");

    var changes = PublicationDiff.between(before, after);

    assertThat(changes, contains(new FieldChange(FIRST_ELEMENT_NAME_PATH, "old", "new")));
  }

  @Test
  void shouldReportRemovedElementAtItsOriginalIndex() {
    var before = arrayOf("[{\"id\":\"a\"},{\"id\":\"b\"}]");
    var after = arrayOf("[{\"id\":\"b\"}]");

    var changes = PublicationDiff.between(before, after);

    assertEquals(1, changes.size());
    assertEquals(FIRST_ELEMENT_PATH, changes.getFirst().path());
    assertNull(changes.getFirst().newValue());
  }

  @Test
  void shouldReportAddedElement() {
    var before = arrayOf("[{\"id\":\"a\"}]");
    var after = arrayOf("[{\"id\":\"a\"},{\"id\":\"b\"}]");

    var changes = PublicationDiff.between(before, after);

    assertEquals(1, changes.size());
    assertNull(changes.getFirst().oldValue());
  }

  private JsonNode arrayOf(String itemsJson) {
    return JsonUtils.dtoObjectMapper.createObjectNode().set("items", parse(itemsJson));
  }

  private JsonNode parse(String json) {
    try {
      return JsonUtils.dtoObjectMapper.readTree(json);
    } catch (Exception exception) {
      throw new IllegalArgumentException(exception);
    }
  }
}
