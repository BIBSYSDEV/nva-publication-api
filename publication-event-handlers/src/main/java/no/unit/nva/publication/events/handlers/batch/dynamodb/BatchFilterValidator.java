package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.PublicationStatus;

public final class BatchFilterValidator {

  private static final Pattern YEAR_PATTERN = Pattern.compile("\\d{4}");
  private static final String INVALID_YEAR_MESSAGE =
      "Invalid publicationYear format. Expected 4-digit year: ";
  private static final String INVALID_STATUS_MESSAGE = "Invalid status value: ";
  private static final String INVALID_IMPORT_SOURCE_MESSAGE = "Invalid import source value: ";

  private BatchFilterValidator() {}

  public static void validate(BatchFilter filter) {
    if (isNull(filter)) {
      return;
    }
    validatePublicationYears(filter.publicationYears());
    validateStatuses(filter.statuses());
    validateImportSources(filter.fileImportSources());
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
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(INVALID_STATUS_MESSAGE + status, exception);
    }
  }

  private static void validateImportSources(Collection<String> importSources) {
    if (isNull(importSources) || importSources.isEmpty()) {
      return;
    }
    importSources.forEach(BatchFilterValidator::validateImportSource);
  }

  private static void validateImportSource(String importSource) {
    var validSources =
        Arrays.stream(ImportSource.Source.values()).map(Enum::name).collect(Collectors.toSet());
    if (!validSources.contains(importSource)) {
      throw new IllegalArgumentException(INVALID_IMPORT_SOURCE_MESSAGE + importSource);
    }
  }
}
