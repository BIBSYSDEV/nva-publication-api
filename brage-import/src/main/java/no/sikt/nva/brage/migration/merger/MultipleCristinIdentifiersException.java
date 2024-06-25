package no.sikt.nva.brage.migration.merger;

public class MultipleCristinIdentifiersException extends RuntimeException {

    public static final String MULTIPLE_CRISTIN_IDENTIFIERS_EXCEPTION_MESSAGE = "Multiple cristin identifiers are not allowed!";
    public MultipleCristinIdentifiersException() {
        super(MULTIPLE_CRISTIN_IDENTIFIERS_EXCEPTION_MESSAGE);
    }
}