package no.unit.nva.cristin.patcher.exception;

import nva.commons.apigateway.exceptions.NotFoundException;

public final class ExceptionHandling {

    private ExceptionHandling() {

    }

    public static RuntimeException castToCorrectRuntimeException(Exception exception) {
        if (exception instanceof NotFoundException) {
            return new no.unit.nva.cristin.patcher.exception.NotFoundException(exception);
        }
        if (exception instanceof ParentPublicationException) {
            return (ParentPublicationException) exception;
        }
        if (exception instanceof PublicationInstanceMismatchException) {
            return (PublicationInstanceMismatchException) exception;
        }
        return new RuntimeException(exception);
    }
}
