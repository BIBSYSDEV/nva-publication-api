package no.unit.nva.publication.adapter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.Context;
import java.util.Optional;

public interface AuthorizerContextProvider {
    Optional<ObjectNode> buildAuthorizerNode(Context ctx);
}
