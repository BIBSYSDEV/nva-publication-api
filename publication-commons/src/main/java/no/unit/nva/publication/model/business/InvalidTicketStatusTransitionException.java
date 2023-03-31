package no.unit.nva.publication.model.business;

public class InvalidTicketStatusTransitionException extends RuntimeException {

    public InvalidTicketStatusTransitionException(String message) {
        super(message);
    }
}
