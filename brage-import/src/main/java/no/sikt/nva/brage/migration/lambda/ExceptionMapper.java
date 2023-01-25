package no.sikt.nva.brage.migration.lambda;

import no.sikt.nva.brage.migration.AssociatedArtifactException;
import no.sikt.nva.brage.migration.merger.MergePublicationException;

public final class ExceptionMapper {

    private ExceptionMapper() {

    }

    public static RuntimeException castToCorrectRuntimeException(Exception exception) {
        if (exception instanceof AssociatedArtifactException) {
            return (AssociatedArtifactException) exception;
        } else if (exception instanceof MissingFieldsException) {
            return (MissingFieldsException) exception;
        } else if (exception instanceof PublicationContextException) {
            return (PublicationContextException) exception;
        } else if (exception instanceof MergePublicationException) {
            return (MergePublicationException) exception;
        } else {
            return new RuntimeException(exception);
        }
    }
}
