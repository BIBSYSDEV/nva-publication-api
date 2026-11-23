package no.unit.nva.publication.scopusmapping.domain;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

/**
 * One written version of a mapping, as kept in its history. {@code correctionContext} is null
 * unless a curator's correction wrote the version. {@code candidates} lists every candidate PIA had
 * for the Scopus identifier, on the version the PIA migration wrote, and is empty otherwise. Each
 * candidate maps a name to a value as text.
 */
public record ScopusMappingHistoryEntry<T extends ScopusMapping>(
    T mapping, ScopusCorrectionContext correctionContext, List<Map<String, String>> candidates) {

  public ScopusMappingHistoryEntry {
    requireNonNull(mapping, "mapping");
    candidates = List.copyOf(candidates);
  }
}
