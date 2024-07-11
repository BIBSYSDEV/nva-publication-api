package no.unit.nva.model.instancetypes.researchdata;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.net.URI;
import java.util.Set;

public class RelatedUris extends UriSet {
    @JsonCreator
    public RelatedUris(Set<URI> uris) {
        super(uris);
    }
}
