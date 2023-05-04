package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.User;
import nva.commons.apigateway.exceptions.NotFoundException;

import java.net.URI;
import java.util.Set;

public interface ResourceExpansionService {

    ExpandedDataEntry expandEntry(Entity dataEntry) throws JsonProcessingException, NotFoundException;

    Set<URI> getOrganizationIds(Entity dataEntry) throws NotFoundException;

    ExpandedPerson enrichPerson(User username) throws JsonProcessingException;
}
