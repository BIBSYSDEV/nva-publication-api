package no.unit.nva.cristin.patcher.exception;

public final class ChildNotAnthologyException extends RuntimeException {

    private ChildNotAnthologyException() {
        super();
    }

    public static String getExceptionName() {
        return ChildNotAnthologyException.class.getSimpleName();
    }
}
