package no.sikt.nva.brage.migration.lambda;

import no.sikt.nva.brage.migration.merger.AssociatedArtifactException;
import no.sikt.nva.brage.migration.merger.UnmappableCristinRecordException;

public final class ExceptionMapper {

    private ExceptionMapper() {

    }

    public static RuntimeException castToCorrectRuntimeException(Exception exception) {
        if (exception instanceof UnmappableCristinRecordException) {
            return (UnmappableCristinRecordException) exception;
        }
        if (exception instanceof AssociatedArtifactException) {
            return (AssociatedArtifactException) exception;
        } else if (exception instanceof MissingFieldsException) {
            return (MissingFieldsException) exception;
        } else if (exception instanceof PublicationContextException) {
            return (PublicationContextException) exception;
        } else {
            return new RuntimeException(exception);
        }
    }
}
