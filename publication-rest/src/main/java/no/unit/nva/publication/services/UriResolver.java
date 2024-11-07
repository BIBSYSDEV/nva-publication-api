package no.unit.nva.publication.services;

import java.net.URI;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public interface UriResolver {

    URI resolve(URI alias) throws ApiGatewayException;
}
