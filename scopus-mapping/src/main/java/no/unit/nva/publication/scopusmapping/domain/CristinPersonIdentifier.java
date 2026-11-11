package no.unit.nva.publication.scopusmapping.domain;

/** The identifier of a person in Cristin. */
public record CristinPersonIdentifier(String value) implements NumericIdentifier {

  public CristinPersonIdentifier {
    NumericIdentifier.requireNumeric(value, "Cristin person identifier");
  }
}
