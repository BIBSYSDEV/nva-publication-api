package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Set;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.storage.model.DataEntry;

public interface ResourceExpansionService {

    ExpandedDataEntry expandEntry(DataEntry dataEntry) throws JsonProcessingException;

    Set<URI> getOrganizationIds(String username);
}
