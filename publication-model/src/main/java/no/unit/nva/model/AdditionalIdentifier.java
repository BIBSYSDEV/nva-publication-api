package no.unit.nva.model;


public record AdditionalIdentifier(String sourceName, String value) implements AdditionalIdentifierBase {

    static final String TYPE = "AdditionalIdentifier";

}
