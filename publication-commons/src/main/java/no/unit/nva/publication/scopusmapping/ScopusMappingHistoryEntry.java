package no.unit.nva.publication.scopusmapping;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

/**
 * One written version of a mapping, read from its {@code HISTORY} item. {@code correctionContext}
 * is null unless a curator correction wrote the version. {@code candidates} lists every PIA
 * candidate on the migrated version and is empty otherwise; each candidate maps attribute names to
 * their string, number or boolean values as text.
 */
public record ScopusMappingHistoryEntry<T>(
    T mapping, ScopusCorrectionContext correctionContext, List<Map<String, String>> candidates) {

  public ScopusMappingHistoryEntry {
    requireNonNull(mapping, "mapping");
    candidates = List.copyOf(candidates);
  }
}
