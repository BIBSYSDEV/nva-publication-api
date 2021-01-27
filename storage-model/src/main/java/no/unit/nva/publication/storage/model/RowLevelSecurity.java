package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;

public interface RowLevelSecurity {

    @JsonIgnore
    URI getCustomerId();

    String getOwner();
}
