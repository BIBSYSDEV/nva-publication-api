package no.unit.nva.publication.events.handlers.dynamodbstream;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.ImportCandidateGenerator.randomImportCandidate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The stream filter on ImportCandidateDynamodbStreamToEventBridgeHandler in template.yaml hardcodes
 * the import candidate key prefix. If the two drift apart, the handler silently gets no events.
 */
class ImportCandidateStreamFilterTest {

  private static final Path TEMPLATE = Path.of("..", "template.yaml");
  private static final String HANDLER_RESOURCE =
      "  ImportCandidateDynamodbStreamToEventBridgeHandler:";
  private static final Pattern FILTER_PATTERN_LINE = Pattern.compile("^\\s*- Pattern: '(.+)'$");
  private static final Pattern NEXT_RESOURCE_LINE = Pattern.compile("^ {2}\\S");

  @Test
  void shouldLetImportCandidateItemsThroughTheStreamFilter() throws IOException {
    var importCandidate = randomImportCandidate();
    var partitionKey =
        new ImportCandidateDao(importCandidate, importCandidate.getIdentifier()).getPartitionKey();
    var prefix = partitionKeyPrefixInStreamFilter();

    assertTrue(
        partitionKey.startsWith(prefix),
        () -> "%s does not start with the filter prefix %s".formatted(partitionKey, prefix));
  }

  @ParameterizedTest
  @ValueSource(strings = {"AUTHOR#SCOPUS#99999990101", "AFFILIATION#SCOPUS#99990101"})
  void shouldKeepScopusMappingItemsOutOfTheStreamFilter(String partitionKey) throws IOException {
    var prefix = partitionKeyPrefixInStreamFilter();

    assertFalse(
        partitionKey.startsWith(prefix),
        () -> "%s starts with the filter prefix %s".formatted(partitionKey, prefix));
  }

  private static String partitionKeyPrefixInStreamFilter() throws IOException {
    var filterPattern =
        dtoObjectMapper.readTree(filterPatternOfHandler(Files.readAllLines(TEMPLATE)));
    return filterPattern.at("/dynamodb/Keys/PK0/S/0/prefix").textValue();
  }

  private static String filterPatternOfHandler(List<String> templateLines) {
    var handlerLine = templateLines.indexOf(HANDLER_RESOURCE);
    return templateLines.subList(handlerLine + 1, templateLines.size()).stream()
        .takeWhile(line -> !NEXT_RESOURCE_LINE.matcher(line).find())
        .map(FILTER_PATTERN_LINE::matcher)
        .filter(Matcher::matches)
        .map(matcher -> matcher.group(1))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "No FilterCriteria pattern on the import candidate stream handler"));
  }
}
