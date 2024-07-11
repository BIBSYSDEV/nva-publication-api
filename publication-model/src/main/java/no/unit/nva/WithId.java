package no.unit.nva;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public interface WithId {

    URI NAMESPACE = URI.create(getIdNamespace());
    String LINKED_DATA_ID_FIELD = "id";

    @JsonProperty(LINKED_DATA_ID_FIELD)
    default URI getId() {
        return toId(getIdentifier());
    }

    default void setId(URI id) {
        // do nothing
    }

    SortableIdentifier getIdentifier();

    private static URI toId(SortableIdentifier identifier) {
        return UriWrapper.fromUri(NAMESPACE).addChild(identifier.toString()).getUri();
    }

    private static String getIdNamespace() {
        return new Environment().readEnv("ID_NAMESPACE");
    }
}
