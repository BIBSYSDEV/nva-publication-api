package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

import java.util.List;
import java.util.Map;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;

/**
 * The stored attributes that only {@code HISTORY} items have. A curator's correction stores where
 * it was made. The PIA migration stores every candidate PIA had, as a list of maps whose values are
 * strings, numbers or booleans.
 */
record HistoryAttributesDao(
    String scopusPublicationIdentifier,
    Integer sequenceNumber,
    List<Map<String, String>> candidates) {

  static HistoryAttributesDao of(ScopusCorrectionContext correctionContext) {
    return new HistoryAttributesDao(
        correctionContext.scopusPublicationIdentifier(), correctionContext.sequenceNumber(), null);
  }

  ScopusCorrectionContext toCorrectionContext() {
    return isNull(scopusPublicationIdentifier)
        ? null
        : new ScopusCorrectionContext(scopusPublicationIdentifier, sequenceNumber);
  }

  List<Map<String, String>> candidatesOrEmpty() {
    return isNull(candidates) ? emptyList() : candidates;
  }
}
