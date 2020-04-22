package no.unit.nva.publication.exception;

import nva.commons.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

public class NotImplementedException extends ApiGatewayException {

    public static final String NOT_IMPLEMENTED = "This method is not implemented.";

    public NotImplementedException() {
        super(NOT_IMPLEMENTED);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_NOT_IMPLEMENTED;
    }
}
