package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.api.Test;

class ResourceDiffTest {

  private static final String FIRST_ELEMENT_PATH = "/items/0";
  private static final String FIRST_ELEMENT_NAME_PATH = "/items/0/name";
  private static final String RELATED_RESOURCES_FIELD = "relatedResources";

  @Test
  void shouldReportFieldThatOnlyExistsOnResourceAndNotOnPublication() {
    var resource = Resource.fromPublication(randomPublication());
    var before = ResourceDiff.snapshot(resource);
    resource.setRelatedResources(List.of(SortableIdentifier.next()));

    var changes = ResourceDiff.between(before, ResourceDiff.snapshot(resource));

    assertEquals(1, changes.size());
    assertThat(changes.getFirst().path(), containsString(RELATED_RESOURCES_FIELD));
  }

  @Test
  void shouldReportNoChangesWhenArrayElementsAreOnlyReordered() {
    var before = arrayOf("[{\"id\":\"a\"},{\"id\":\"b\"},{\"id\":\"c\"}]");
    var after = arrayOf("[{\"id\":\"c\"},{\"id\":\"a\"},{\"id\":\"b\"}]");

    assertThat(ResourceDiff.between(before, after), empty());
  }

  @Test
  void shouldReportChangedElementOnlyWhenOtherElementsAreReordered() {
    var before = arrayOf("[{\"id\":\"a\",\"name\":\"old\"},{\"id\":\"b\"},{\"id\":\"c\"}]");
    var after = arrayOf("[{\"id\":\"c\"},{\"id\":\"b\"},{\"id\":\"a\",\"name\":\"new\"}]");

    var changes = ResourceDiff.between(before, after);

    assertThat(changes, contains(new FieldChange(FIRST_ELEMENT_NAME_PATH, "old", "new")));
  }

  @Test
  void shouldReportRemovedElementAtItsOriginalIndex() {
    var before = arrayOf("[{\"id\":\"a\"},{\"id\":\"b\"}]");
    var after = arrayOf("[{\"id\":\"b\"}]");

    var changes = ResourceDiff.between(before, after);

    assertEquals(1, changes.size());
    assertEquals(FIRST_ELEMENT_PATH, changes.getFirst().path());
    assertNull(changes.getFirst().newValue());
  }

  @Test
  void shouldReportAddedElement() {
    var before = arrayOf("[{\"id\":\"a\"}]");
    var after = arrayOf("[{\"id\":\"a\"},{\"id\":\"b\"}]");

    var changes = ResourceDiff.between(before, after);

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
