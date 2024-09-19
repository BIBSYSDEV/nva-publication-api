package no.unit.nva.model.additionalidentifiers;


public record AdditionalIdentifier(String sourceName, String value) implements AdditionalIdentifierBase {

    static final String TYPE = "AdditionalIdentifier";

}
