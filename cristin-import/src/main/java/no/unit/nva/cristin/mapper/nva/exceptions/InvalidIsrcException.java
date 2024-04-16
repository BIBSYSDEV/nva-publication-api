package no.unit.nva.cristin.mapper.nva.exceptions;

public final class InvalidIsrcException extends RuntimeException {

    private InvalidIsrcException() {
        super();
    }

    public static String name() {
        return InvalidIsrcException.class.getSimpleName();
    }
}
