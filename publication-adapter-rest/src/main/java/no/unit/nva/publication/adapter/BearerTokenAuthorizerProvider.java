package no.unit.nva.publication.adapter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.Context;
import java.util.Optional;

/**
 * Supplies no authorizer context, which is the point: an empty one would count as proof that a
 * gateway had already authorized the request and suppress the JWKS validation nva-commons performs.
 * Leaving it out makes the bearer token the only thing a caller is trusted on.
 */
public final class BearerTokenAuthorizerProvider implements AuthorizerContextProvider {

    @Override
    public Optional<ObjectNode> buildAuthorizerNode(Context context) {
        return Optional.empty();
    }
}
