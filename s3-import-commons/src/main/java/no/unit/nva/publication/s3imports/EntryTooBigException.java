package no.unit.nva.publication.s3imports;

public class EntryTooBigException extends RuntimeException {

    public static final String MESSAGE = "Cristin Entry too big:";

    public EntryTooBigException(String entryDetail) {
        super(MESSAGE + entryDetail);
    }
}
