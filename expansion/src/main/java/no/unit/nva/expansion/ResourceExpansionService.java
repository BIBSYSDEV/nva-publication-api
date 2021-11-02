package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Set;
import no.unit.nva.expansion.model.ExpandedDatabaseEntry;
import no.unit.nva.publication.storage.model.ResourceUpdate;

public interface ResourceExpansionService {

    ExpandedDatabaseEntry expandEntry(ResourceUpdate resourceUpdate) throws JsonProcessingException;

    Set<URI> getOrganizationIds(String username);
}
