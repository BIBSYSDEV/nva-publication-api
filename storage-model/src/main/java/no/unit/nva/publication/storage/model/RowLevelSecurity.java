package no.unit.nva.publication.storage.model;

import java.net.URI;

public interface RowLevelSecurity {

    URI getCustomerId();

    String getOwner();
}
