package no.unit.nva.publication.fetch;

import java.net.URI;
import nva.commons.apigateway.exceptions.RedirectException;

public class RedirectToLandingPageException extends RedirectException {
    private final URI location;
    private final int httpStatusCode;

    public RedirectToLandingPageException(URI location, int httpStatusCode) {
        super("Redirection");
        this.location = location;
        this.httpStatusCode = httpStatusCode;
    }

    @Override
    public URI getLocation() {
        return location;
    }

    @Override
    protected Integer statusCode() {
        return httpStatusCode;
    }
}
