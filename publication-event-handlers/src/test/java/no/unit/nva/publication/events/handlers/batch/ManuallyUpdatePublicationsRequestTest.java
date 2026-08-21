package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.publication.events.handlers.batch.ManualUpdateType.PUBLISHER;
import static no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest.DEFAULT_LIMIT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ManuallyUpdatePublicationsRequestTest {

  private static final Integer NO_LIMIT = null;
  private static final int ZERO_LIMIT = 0;
  private static final int NEGATIVE_LIMIT = -1;
  private static final int SINGLE_RESOURCE = 1;
  private static final int TWO_RESOURCES = 2;
  private static final String SIZE_PARAM = "size";

  @Test
  void shouldRejectRequestWithoutDryRun() {
    assertThrows(IllegalArgumentException.class, () -> createRequest(null, NO_LIMIT));
  }

  @Test
  void shouldRejectLimitBelowOneResource() {
    assertThrows(IllegalArgumentException.class, () -> createRequest(true, ZERO_LIMIT));
    assertThrows(IllegalArgumentException.class, () -> createRequest(true, NEGATIVE_LIMIT));
  }

  @Test
  void shouldLimitChangesToRequestedNumberOfResources() {
    assertEquals(SINGLE_RESOURCE, createRequest(true, SINGLE_RESOURCE).maxChanges());
  }

  @Test
  void shouldFallBackToDefaultLimitWhenNeitherLimitNorSizeIsProvided() {
    assertEquals(DEFAULT_LIMIT, createRequest(true, NO_LIMIT).maxChanges());
  }

  @Test
  void shouldUseSizeSearchParamAsLimit() {
    var request = createRequestWithSize(String.valueOf(SINGLE_RESOURCE));

    assertEquals(SINGLE_RESOURCE, request.maxChanges());
  }

  @Test
  void shouldPreferLimitOverSizeSearchParam() {
    var request =
        new ManuallyUpdatePublicationsRequest(
            PUBLISHER,
            randomString(),
            randomString(),
            Map.of(SIZE_PARAM, String.valueOf(SINGLE_RESOURCE)),
            null,
            true,
            TWO_RESOURCES);

    assertEquals(TWO_RESOURCES, request.maxChanges());
  }

  @Test
  void shouldRejectSizeSearchParamBelowOneResource() {
    assertThrows(
        IllegalArgumentException.class, () -> createRequestWithSize(String.valueOf(ZERO_LIMIT)));
  }

  @Test
  void shouldRejectSizeSearchParamThatIsNotANumber() {
    assertThrows(IllegalArgumentException.class, () -> createRequestWithSize(randomString()));
  }

  @Test
  void shouldTreatMissingSearchParamsAsNoFilter() {
    var request =
        new ManuallyUpdatePublicationsRequest(
            PUBLISHER, randomString(), randomString(), null, null, true, NO_LIMIT);

    assertEquals(Map.of(), request.searchParams());
    assertEquals(DEFAULT_LIMIT, request.maxChanges());
  }

  private static ManuallyUpdatePublicationsRequest createRequest(Boolean dryRun, Integer limit) {
    return new ManuallyUpdatePublicationsRequest(
        PUBLISHER, randomString(), randomString(), Map.of(), null, dryRun, limit);
  }

  private static ManuallyUpdatePublicationsRequest createRequestWithSize(String size) {
    return new ManuallyUpdatePublicationsRequest(
        PUBLISHER, randomString(), randomString(), Map.of(SIZE_PARAM, size), null, true, NO_LIMIT);
  }
}
