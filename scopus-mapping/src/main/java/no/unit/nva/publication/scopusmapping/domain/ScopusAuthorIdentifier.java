package no.unit.nva.publication.scopusmapping.domain;

/** The identifier Scopus gives an author (AUID). */
public record ScopusAuthorIdentifier(String value) implements NumericIdentifier {

  public ScopusAuthorIdentifier {
    NumericIdentifier.requireNumeric(value, "Scopus author identifier");
  }
}
