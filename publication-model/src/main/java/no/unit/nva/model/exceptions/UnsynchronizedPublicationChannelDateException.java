package no.unit.nva.model.exceptions;

public class UnsynchronizedPublicationChannelDateException extends RuntimeException {

    public UnsynchronizedPublicationChannelDateException() {
        super("Publication date year and year in publication channel URI must match");
    }
}
