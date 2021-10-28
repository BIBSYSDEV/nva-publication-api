package no.unit.nva.expansion;

import java.net.URI;
import java.util.Set;

public interface WithOrganizationScope {

    Set<URI> getOrganizationIds();

    void setOrganizationIds(Set<URI> organizationIds);

}


