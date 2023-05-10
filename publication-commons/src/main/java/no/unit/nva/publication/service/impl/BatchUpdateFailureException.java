package no.unit.nva.publication.service.impl;

import java.util.List;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class BatchUpdateFailureException extends RuntimeException {

  public BatchUpdateFailureException(List<String> failingEntriesIdentifiers) {
    super(createMessage(failingEntriesIdentifiers));
  }

  private static String createMessage(List<String> failingEntriesIdentifiers) {
    var identities = String.join(",", failingEntriesIdentifiers);
    return String.format("BatchUpdate failure for %s", identities);
  }
}
