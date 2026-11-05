package no.unit.nva.publication.scopusmapping;

import static java.util.Objects.requireNonNull;

import java.util.regex.Pattern;

/** Scopus and Cristin ids in mappings are plain numeric ids, never URIs. */
final class ScopusMappingIds {

  private static final Pattern NUMERIC_ID = Pattern.compile("\\d+");

  private ScopusMappingIds() {}

  static String requireNumericId(String id, String name) {
    requireNonNull(id, name);
    if (!NUMERIC_ID.matcher(id).matches()) {
      throw new IllegalArgumentException("%s must be a plain numeric id: %s".formatted(name, id));
    }
    return id;
  }
}
