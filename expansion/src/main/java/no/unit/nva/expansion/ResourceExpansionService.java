package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Set;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.model.business.Entity;
import nva.commons.apigateway.exceptions.NotFoundException;

public interface ResourceExpansionService {
    
    ExpandedDataEntry expandEntry(Entity dataEntry) throws JsonProcessingException, NotFoundException;
    
    Set<URI> getOrganizationIds(Entity dataEntry) throws NotFoundException;
}
