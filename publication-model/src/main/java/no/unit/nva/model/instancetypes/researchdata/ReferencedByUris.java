package no.unit.nva.model.instancetypes.researchdata;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.net.URI;
import java.util.Set;

public class ReferencedByUris extends UriSet {
    @JsonCreator
    public ReferencedByUris(Set<URI> uris) {
        super(uris);
    }
}
