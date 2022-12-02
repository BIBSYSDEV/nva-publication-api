package no.unit.nva.publication.exception;

import nva.commons.apigateway.exceptions.ConflictException;
import org.apache.http.HttpStatus;

public class InvalidPublicationException extends ConflictException {

    public InvalidPublicationException(String message) {
        super(message);
    }
    
    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_CONFLICT;
    }
}
