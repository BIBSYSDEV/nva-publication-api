package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import java.util.Collection;
import java.util.regex.Pattern;
import no.unit.nva.model.PublicationStatus;

public final class BatchFilterValidator {

  private static final Pattern YEAR_PATTERN = Pattern.compile("\\d{4}");
  private static final String INVALID_YEAR_MESSAGE =
      "Invalid publicationYear format. Expected 4-digit year: ";
  private static final String INVALID_STATUS_MESSAGE = "Invalid status value: ";

  private BatchFilterValidator() {}

  public static void validate(BatchFilter filter) {
    if (isNull(filter)) {
      return;
    }
    validatePublicationYears(filter.publicationYears());
    validateStatuses(filter.statuses());
  }

  private static void validatePublicationYears(Collection<String> years) {
    if (isNull(years) || years.isEmpty()) {
      return;
    }
    years.forEach(BatchFilterValidator::validatePublicationYear);
  }

  private static void validatePublicationYear(String year) {
    if (!YEAR_PATTERN.matcher(year).matches()) {
      throw new IllegalArgumentException(INVALID_YEAR_MESSAGE + year);
    }
  }

  private static void validateStatuses(Collection<String> statuses) {
    if (isNull(statuses) || statuses.isEmpty()) {
      return;
    }
    statuses.forEach(BatchFilterValidator::validateStatus);
  }

  private static void validateStatus(String status) {
    try {
      PublicationStatus.lookup(status);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(INVALID_STATUS_MESSAGE + status, e);
    }
  }
}
