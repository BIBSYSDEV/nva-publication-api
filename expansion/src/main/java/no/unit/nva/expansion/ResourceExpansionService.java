package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.storage.model.DataEntry;
import nva.commons.apigateway.exceptions.NotFoundException;

import java.net.URI;
import java.util.Set;

public interface ResourceExpansionService {

    ExpandedDataEntry expandEntry(DataEntry dataEntry) throws JsonProcessingException, NotFoundException;

    Set<URI> getOrganizationIds(DataEntry dataEntry) throws NotFoundException;

}
