package no.unit.nva.cristin.mapper;

public final class MissingFieldsException extends RuntimeException {

    private MissingFieldsException() {
        super();
    }

    public static String name() {
        return MissingFieldsException.class.getSimpleName();
    }
}
