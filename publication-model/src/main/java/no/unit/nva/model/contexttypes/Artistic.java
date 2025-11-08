package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Artistic implements PublicationContext {
    @JacocoGenerated
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Artistic;
    }

    @JsonIgnore
    @Override
    public Set<URI> extractPublicationContextUris() {
        return Collections.emptySet();
    }
}
