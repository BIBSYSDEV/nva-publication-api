package no.unit.nva.publication.model.business.importcandidate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class NotImported implements ImportStatus {

    public NotImported() {

    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        return o instanceof NotImported;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return 0;
    }
}
