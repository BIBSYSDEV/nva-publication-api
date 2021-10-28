package no.unit.nva.expansion.restclients;

import java.net.URI;
import java.util.Set;

public interface InstitutionClient {

    Set<URI> getOrganizationIds(URI organizationId);

}
