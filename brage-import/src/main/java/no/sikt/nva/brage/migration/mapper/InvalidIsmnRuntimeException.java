package no.sikt.nva.brage.migration.mapper;

public class InvalidIsmnRuntimeException extends RuntimeException {

    public InvalidIsmnRuntimeException(Exception e) {
        super(e);
    }
}
