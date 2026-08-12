package no.unit.nva.publication.events.handlers.batch;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.ioutils.IoUtils;

public record ManuallyUpdatePublicationsRequest(
    ManualUpdateType type,
    String oldValue,
    String newValue,
    Map<String, String> searchParams,
    Comparator comparator,
    Boolean dryRun)
    implements JsonSerializable {

  public static final String MISSING_DRY_RUN_MESSAGE =
      "Field 'dryRun' is required: set it to true to preview the changes without writing, "
          + "or false to apply them. Note that the field name is case sensitive.";

  public ManuallyUpdatePublicationsRequest {
    if (isNull(dryRun)) {
      throw new IllegalArgumentException(MISSING_DRY_RUN_MESSAGE);
    }
  }

  public static ManuallyUpdatePublicationsRequest fromInputStream(InputStream inputStream)
      throws JsonProcessingException {
    return JsonUtils.dtoObjectMapper.readValue(
        IoUtils.streamToString(inputStream), ManuallyUpdatePublicationsRequest.class);
  }

  public boolean isDryRun() {
    return Boolean.TRUE.equals(dryRun);
  }
}
