package no.unit.nva.cristin.patcher.exception;

public final class ChildPatchPublicationInstanceMismatchException extends RuntimeException {

    private ChildPatchPublicationInstanceMismatchException() {
        super();
    }

    public static String getExceptionName() {
        return ChildPatchPublicationInstanceMismatchException.class.getSimpleName();
    }
}
