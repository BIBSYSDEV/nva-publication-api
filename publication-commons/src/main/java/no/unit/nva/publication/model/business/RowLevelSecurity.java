package no.unit.nva.publication.model.business;

import java.net.URI;

public interface RowLevelSecurity {
    
    URI getCustomerId();
    
    String getOwner();
}
