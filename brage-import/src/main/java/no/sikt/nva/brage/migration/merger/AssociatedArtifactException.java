package no.sikt.nva.brage.migration.merger;

public class AssociatedArtifactException extends RuntimeException {

    public AssociatedArtifactException(String message, Exception exception) {
        super(message, exception);
    }

}
