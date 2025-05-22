package no.unit.nva.cristin.mapper.nva.exceptions;

public final class UnconfirmedSeriesException extends RuntimeException {

    private UnconfirmedSeriesException() {
        super();
    }

    public static String name() {
        return UnconfirmedSeriesException.class.getSimpleName();
    }

}
