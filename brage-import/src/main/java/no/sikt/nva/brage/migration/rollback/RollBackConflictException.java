package no.sikt.nva.brage.migration.rollback;

public class RollBackConflictException extends RuntimeException {

    public RollBackConflictException(String message) {
        super(message);
    }
}
