package no.unit.nva.publication.exception;

public class UnsupportedPublicationStatusTransition extends UnsupportedOperationException {

    public UnsupportedPublicationStatusTransition(String message) {
        super(message);
    }
}
