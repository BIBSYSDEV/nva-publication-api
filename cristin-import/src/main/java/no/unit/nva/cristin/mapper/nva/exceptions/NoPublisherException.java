package no.unit.nva.cristin.mapper.nva.exceptions;

public final class NoPublisherException extends RuntimeException {

    private NoPublisherException() {
        super();
    }

    public static String name() {
        return NoPublisherException.class.getSimpleName();
    }
}
