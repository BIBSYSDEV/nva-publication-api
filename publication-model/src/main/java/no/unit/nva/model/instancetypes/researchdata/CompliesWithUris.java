package no.unit.nva.model.instancetypes.researchdata;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.net.URI;
import java.util.Set;

public class CompliesWithUris extends UriSet {
    @JsonCreator
    public CompliesWithUris(Set<URI> uris) {
        super(uris);
    }
}
