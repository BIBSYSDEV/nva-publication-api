package no.unit.nva.cristin.mapper.nva.exceptions;

public final class UnconfirmedPublisherException extends RuntimeException {

    private UnconfirmedPublisherException() {
        super();
    }

    public static String name() {
        return UnconfirmedPublisherException.class.getSimpleName();
    }
}
