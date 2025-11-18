package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

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
}
