package no.unit.nva.cristin.mapper.nva.exceptions;

public class CristinMuseumCategoryException extends RuntimeException {

    private static final String FORMAT_MESSAGE = "Unsupported museum category: %s";

    public CristinMuseumCategoryException(String category) {
        super(String.format(FORMAT_MESSAGE, category));
    }
}
