package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.publication.events.handlers.batch.ManualUpdateType.PUBLISHER;
import static no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest.DEFAULT_LIMIT;
import static no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest.DEFAULT_PAGE_SIZE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ManuallyUpdatePublicationsRequestTest {

  private static final Integer NO_LIMIT = null;
  private static final Integer NO_PAGE_SIZE = null;
  private static final int ZERO_LIMIT = 0;
  private static final int NEGATIVE_LIMIT = -1;
  private static final int SINGLE_RESOURCE = 1;
  private static final int TWO_RESOURCES = 2;
  private static final int SMALL_PAGE_SIZE = 4;
  private static final int TOO_LARGE_PAGE_SIZE = 1_001;
  private static final int LIMIT_ABOVE_DEFAULT_PAGE_SIZE = 1_000;
  private static final int ZERO_PAGE_SIZE = 0;
  private static final String SIZE_PARAM = "size";
  private static final String BLANK = " ";
  private static final Map<String, String> SEARCH_PARAMS = Map.of("query", randomString());

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
            TWO_RESOURCES,
            NO_PAGE_SIZE);

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
  void shouldRejectMissingSearchParams() {
    assertThrows(IllegalArgumentException.class, () -> createRequestWithSearchParams(null));
    assertThrows(IllegalArgumentException.class, () -> createRequestWithSearchParams(Map.of()));
  }

  @Test
  void shouldRejectBlankOldValue() {
    assertThrows(
        IllegalArgumentException.class, () -> createRequestWithValues(BLANK, randomString()));
    assertThrows(
        IllegalArgumentException.class, () -> createRequestWithValues(null, randomString()));
  }

  @Test
  void shouldRejectBlankNewValue() {
    assertThrows(
        IllegalArgumentException.class, () -> createRequestWithValues(randomString(), BLANK));
    assertThrows(
        IllegalArgumentException.class, () -> createRequestWithValues(randomString(), null));
  }

  @Test
  void shouldFallBackToDefaultPageSizeWhenNotProvided() {
    var request = createRequest(true, LIMIT_ABOVE_DEFAULT_PAGE_SIZE);

    assertEquals(DEFAULT_PAGE_SIZE, request.searchPageSize());
  }

  @Test
  void shouldNotRequestLargerPagesThanTheDefaultLimitAllows() {
    assertEquals(DEFAULT_LIMIT, createRequest(true, NO_LIMIT).searchPageSize());
  }

  @Test
  void shouldUseRequestedPageSize() {
    assertEquals(SMALL_PAGE_SIZE, createRequestWithPageSize(SMALL_PAGE_SIZE).searchPageSize());
  }

  @Test
  void shouldNotRequestLargerPagesThanTheLimitAllows() {
    var request =
        new ManuallyUpdatePublicationsRequest(
            PUBLISHER,
            randomString(),
            randomString(),
            SEARCH_PARAMS,
            null,
            true,
            SINGLE_RESOURCE,
            DEFAULT_PAGE_SIZE);

    assertEquals(SINGLE_RESOURCE, request.searchPageSize());
  }

  @Test
  void shouldRejectPageSizeOutsideWhatTheSearchApiAccepts() {
    assertThrows(IllegalArgumentException.class, () -> createRequestWithPageSize(ZERO_PAGE_SIZE));
    assertThrows(
        IllegalArgumentException.class, () -> createRequestWithPageSize(TOO_LARGE_PAGE_SIZE));
  }

  private static ManuallyUpdatePublicationsRequest createRequest(Boolean dryRun, Integer limit) {
    return new ManuallyUpdatePublicationsRequest(
        PUBLISHER,
        randomString(),
        randomString(),
        SEARCH_PARAMS,
        null,
        dryRun,
        limit,
        NO_PAGE_SIZE);
  }

  private static ManuallyUpdatePublicationsRequest createRequestWithSearchParams(
      Map<String, String> searchParams) {
    return new ManuallyUpdatePublicationsRequest(
        PUBLISHER,
        randomString(),
        randomString(),
        searchParams,
        null,
        true,
        NO_LIMIT,
        NO_PAGE_SIZE);
  }

  private static ManuallyUpdatePublicationsRequest createRequestWithValues(
      String oldValue, String newValue) {
    return new ManuallyUpdatePublicationsRequest(
        PUBLISHER, oldValue, newValue, SEARCH_PARAMS, null, true, NO_LIMIT, NO_PAGE_SIZE);
  }

  private static ManuallyUpdatePublicationsRequest createRequestWithSize(String size) {
    return new ManuallyUpdatePublicationsRequest(
        PUBLISHER,
        randomString(),
        randomString(),
        Map.of(SIZE_PARAM, size),
        null,
        true,
        NO_LIMIT,
        NO_PAGE_SIZE);
  }

  private static ManuallyUpdatePublicationsRequest createRequestWithPageSize(int pageSize) {
    return new ManuallyUpdatePublicationsRequest(
        PUBLISHER, randomString(), randomString(), SEARCH_PARAMS, null, true, NO_LIMIT, pageSize);
  }
}
