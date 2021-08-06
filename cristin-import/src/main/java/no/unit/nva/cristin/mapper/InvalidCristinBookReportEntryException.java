package no.unit.nva.cristin.mapper;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class InvalidCristinBookReportEntryException extends RuntimeException {

    private static String ERROR_TEMPLATE = "The field %s in CristinBookReport, contains an invalid value %s";

    public InvalidCristinBookReportEntryException(String fieldName, String value) {
        super(String.format(ERROR_TEMPLATE, fieldName, value));
    }

}
