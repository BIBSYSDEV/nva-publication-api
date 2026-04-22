package no.unit.nva.publication.adapter;

import nva.commons.apigateway.ApiGatewayHandler;

@FunctionalInterface
public interface HandlerFactory {
    ApiGatewayHandler<?, ?> create(HandlerContainer container);
}
