package no.unit.nva.model.exceptions;

import no.unit.nva.model.PublicationStatus;

public class InvalidPublicationStatusTransitionException extends Exception {

    public static final String ERROR_MSG_TEMPLATE = "Invalid Publication status transition: %s -> %s";

    public InvalidPublicationStatusTransitionException(PublicationStatus currentStatus, PublicationStatus nextStatus) {
        super(String.format(ERROR_MSG_TEMPLATE, currentStatus.getValue(), nextStatus.getValue()));
    }
}
