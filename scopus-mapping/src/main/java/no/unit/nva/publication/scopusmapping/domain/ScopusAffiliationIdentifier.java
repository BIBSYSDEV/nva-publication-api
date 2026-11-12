package no.unit.nva.publication.scopusmapping.domain;

/** The identifier Scopus gives an affiliation (AFID). */
public record ScopusAffiliationIdentifier(String value) implements NumericIdentifier {

  public ScopusAffiliationIdentifier {
    NumericIdentifier.requireNumeric(value, "Scopus affiliation identifier");
  }
}
