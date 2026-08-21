package no.unit.nva.publication.events.handlers.batch;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.StringUtils;
import nva.commons.core.ioutils.IoUtils;

public record ManuallyUpdatePublicationsRequest(
    ManualUpdateType type,
    String oldValue,
    String newValue,
    Map<String, String> searchParams,
    Comparator comparator,
    Boolean dryRun,
    Integer limit,
    Integer pageSize)
    implements JsonSerializable {

  public static final String MISSING_DRY_RUN_MESSAGE =
      "Field 'dryRun' is required: set it to true to preview the changes without writing, "
          + "or false to apply them. Note that the field name is case sensitive.";
  public static final String INVALID_LIMIT_MESSAGE =
      "Field 'limit' must be a positive number of resources to change before the run stops. "
          + "The search parameter 'size' is accepted as an alias for 'limit'.";
  public static final String INVALID_PAGE_SIZE_MESSAGE =
      "Field 'pageSize' must be between %d and %d hits, which is what the search api accepts.";
  public static final int DEFAULT_LIMIT = 10_000;
  public static final int DEFAULT_PAGE_SIZE = 100;
  private static final String SIZE_PARAM = "size";
  private static final int SMALLEST_LIMIT = 1;
  private static final int SMALLEST_PAGE_SIZE = 1;
  private static final int LARGEST_PAGE_SIZE = 1_000;

  public ManuallyUpdatePublicationsRequest {
    if (isNull(dryRun)) {
      throw new IllegalArgumentException(MISSING_DRY_RUN_MESSAGE);
    }
    validateLimit(requestedLimit(limit, searchParams));
    validatePageSize(pageSize);
  }

  public static ManuallyUpdatePublicationsRequest fromInputStream(InputStream inputStream)
      throws JsonProcessingException {
    return JsonUtils.dtoObjectMapper.readValue(
        IoUtils.streamToString(inputStream), ManuallyUpdatePublicationsRequest.class);
  }

  @Override
  public Map<String, String> searchParams() {
    return isNull(searchParams) ? Map.of() : searchParams;
  }

  public boolean isDryRun() {
    return Boolean.TRUE.equals(dryRun);
  }

  public int maxChanges() {
    var requestedLimit = requestedLimit(limit, searchParams);
    return isNull(requestedLimit) ? DEFAULT_LIMIT : requestedLimit;
  }

  public int searchPageSize() {
    var requestedPageSize = isNull(pageSize) ? DEFAULT_PAGE_SIZE : pageSize;
    return Math.min(requestedPageSize, maxChanges());
  }

  private static Integer requestedLimit(Integer limit, Map<String, String> searchParams) {
    return nonNull(limit) ? limit : sizeSearchParam(searchParams);
  }

  private static Integer sizeSearchParam(Map<String, String> searchParams) {
    return Optional.ofNullable(searchParams)
        .map(params -> params.get(SIZE_PARAM))
        .filter(StringUtils::isNotBlank)
        .map(ManuallyUpdatePublicationsRequest::parseLimit)
        .orElse(null);
  }

  private static Integer parseLimit(String size) {
    return attempt(() -> Integer.parseInt(size))
        .orElseThrow(failure -> new IllegalArgumentException(INVALID_LIMIT_MESSAGE));
  }

  private static void validateLimit(Integer requestedLimit) {
    if (nonNull(requestedLimit) && requestedLimit < SMALLEST_LIMIT) {
      throw new IllegalArgumentException(INVALID_LIMIT_MESSAGE);
    }
  }

  private static void validatePageSize(Integer pageSize) {
    if (nonNull(pageSize) && (pageSize < SMALLEST_PAGE_SIZE || pageSize > LARGEST_PAGE_SIZE)) {
      throw new IllegalArgumentException(
          INVALID_PAGE_SIZE_MESSAGE.formatted(SMALLEST_PAGE_SIZE, LARGEST_PAGE_SIZE));
    }
  }
}
