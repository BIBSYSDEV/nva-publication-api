package no.unit.nva.doi.handler.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class DependencyRemoteNvaApiException extends RuntimeException {
    public static final String EXCEPTION_MESSAGE = "NVA API dependency exception";

    public DependencyRemoteNvaApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DependencyRemoteNvaApiException wrap(ApiGatewayException upstreamException) {
        return new DependencyRemoteNvaApiException(EXCEPTION_MESSAGE, upstreamException);
    }
}
