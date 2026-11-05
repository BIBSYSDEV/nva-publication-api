package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR_ID;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.with;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class ScopusIdentifierMappingRepositoryTest extends ResourcesLocalTest {

  private static final String TABLE_NAME = "import-candidates";
  private static final String OTHER_SCOPUS_AUTHOR_ID = "99999990202";

  private ScopusIdentifierMappingRepository repository;

  @BeforeEach
  void setUp() {
    super.init(TABLE_NAME);
    repository = new ScopusIdentifierMappingRepository(client, TABLE_NAME);
  }

  @Test
  void shouldReadMoreIdsThanFitInOneBatchGetItem() {
    var scopusAuthorIds =
        IntStream.range(0, 150).mapToObj(index -> "999999" + (10_000 + index)).toList();
    scopusAuthorIds.forEach(
        scopusAuthorId -> putItem(toCurrentItem(migratedAuthorMapping(scopusAuthorId))));

    var mappings = repository.fetchCurrentMappings(scopusAuthorIds, List.of("99990301"));

    assertThat(mappings.authorMappings()).containsOnlyKeys(scopusAuthorIds);
    assertThat(mappings.affiliationMappings()).isEmpty();
  }

  @Test
  void shouldAcceptDuplicateAndNullIds() {
    var mapping = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    putItem(toCurrentItem(mapping));

    var mappings =
        repository.fetchCurrentMappings(
            Arrays.asList(SCOPUS_AUTHOR_ID, null, SCOPUS_AUTHOR_ID), List.of("1", "1"));

    assertThat(mappings.authorMappings()).isEqualTo(Map.of(SCOPUS_AUTHOR_ID, mapping));
  }

  @Test
  void shouldReturnNoMappingsWhenAskedForNoIds() {
    var mappings = repository.fetchCurrentMappings(emptyList(), emptyList());

    assertThat(mappings.authorMappings()).isEmpty();
    assertThat(mappings.affiliationMappings()).isEmpty();
  }

  @Test
  void shouldLeaveOutMalformedItemButReadTheOtherIds() {
    var otherMapping = migratedAuthorMapping(OTHER_SCOPUS_AUTHOR_ID);
    putItem(
        with(
            toCurrentItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID)),
            "source",
            fromS("SOMETHING_NEW")));
    putItem(toCurrentItem(otherMapping));

    var mappings =
        repository.fetchCurrentMappings(
            List.of(SCOPUS_AUTHOR_ID, OTHER_SCOPUS_AUTHOR_ID), emptyList());

    assertThat(mappings.authorMappings()).isEqualTo(Map.of(OTHER_SCOPUS_AUTHOR_ID, otherMapping));
  }

  private void putItem(Map<String, AttributeValue> item) {
    client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
  }
}
