package no.unit.nva.publication.model;

import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public record ResourceWithId(URI id) implements JsonSerializable {

    public SortableIdentifier getIdentifier() {
        return new SortableIdentifier(UriWrapper.fromUri(id).getLastPathElement());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }
}
