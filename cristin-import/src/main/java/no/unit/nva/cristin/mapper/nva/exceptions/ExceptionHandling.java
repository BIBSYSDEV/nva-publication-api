package no.unit.nva.cristin.mapper.nva.exceptions;

import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.core.JacocoGenerated;

public final class ExceptionHandling {
    
    @JacocoGenerated
    public ExceptionHandling() {
    
    }
    
    @SuppressWarnings("PMD.NPathComplexity")
    public static RuntimeException castToCorrectRuntimeException(Exception exception) {
        if (exception instanceof InvalidIssnRuntimeException) {
            return (InvalidIssnRuntimeException) exception;
        }
        if (exception instanceof InvalidIsbnRuntimeException) {
            return (InvalidIsbnRuntimeException) exception;
        }
        if (exception instanceof InvalidIsbnException) {
            return new InvalidIsbnRuntimeException(exception);
        }
        if (exception instanceof MissingContributorsException) {
            return (MissingContributorsException) exception;
        }
        if (exception instanceof HrcsException) {
            return (HrcsException) exception;
        }
        if (exception instanceof NoPublisherException) {
            return (NoPublisherException) exception;
        }
        if (exception instanceof UnsupportedMainCategoryException) {
            return (UnsupportedMainCategoryException) exception;
        }
        if (exception instanceof UnsupportedSecondaryCategoryException) {
            return (UnsupportedSecondaryCategoryException) exception;
        }
        if (exception instanceof UnsupportedRoleException) {
            return (UnsupportedRoleException) exception;
        }
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        }
        return new RuntimeException(exception);
    }
    
    public static RuntimeException handlePublicationContextFailure(Exception exception) {
        if (exception instanceof InvalidIssnException) {
            return new InvalidIssnRuntimeException(exception);
        }
        if (exception instanceof InvalidIsbnException) {
            return new InvalidIsbnRuntimeException(exception);
        }
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        }
        return new RuntimeException();
    }
}
