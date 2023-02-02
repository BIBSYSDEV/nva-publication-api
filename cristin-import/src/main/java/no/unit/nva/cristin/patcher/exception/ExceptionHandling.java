package no.unit.nva.cristin.patcher.exception;

import nva.commons.apigateway.exceptions.NotFoundException;

public class ExceptionHandling {

    private ExceptionHandling() {

    }

    public static RuntimeException castToCorrectRuntimeException(Exception exception) {
        if (exception instanceof NotFoundException) {
            return new no.unit.nva.cristin.patcher.exception.NotFoundException(exception);
        }
        if (exception instanceof ParentPublicationException) {
            return (ParentPublicationException) exception;
        }
        return new RuntimeException(exception);
    }
}
