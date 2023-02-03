package no.sikt.nva.scopus.exception;

public final class ExceptionMapper {

    private ExceptionMapper() {

    }

    public static RuntimeException castToCorrectRuntimeException(Exception exception) {
        if (exception instanceof UnsupportedCitationTypeException) {
            return (UnsupportedCitationTypeException) exception;
        } else if (exception instanceof UnsupportedSrcTypeException) {
            return (UnsupportedSrcTypeException) exception;
        } else {
            return new RuntimeException(exception);
        }
    }
}
