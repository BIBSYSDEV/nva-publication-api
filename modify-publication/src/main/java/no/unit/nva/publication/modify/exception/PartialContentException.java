package no.unit.nva.publication.modify.exception;

import nva.commons.exceptions.ApiGatewayException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

public class PartialContentException extends ApiGatewayException {

    public static final String PARTIAL_CONTENT_MESSAGE = "The request contained a Content-Range header indicating that "
            + "the request contains partial content, this method (PUT) only accepts full representations, "
            + "c.f. <https://tools.ietf.org/html/rfc7231#section-4.3.4>";

    public PartialContentException() {
        super(PARTIAL_CONTENT_MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return SC_BAD_REQUEST;
    }
}
