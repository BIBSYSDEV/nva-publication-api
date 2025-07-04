package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Optional;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedOrganization;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.User;
import nva.commons.apigateway.exceptions.NotFoundException;

public interface ResourceExpansionService {

    Optional<ExpandedDataEntry> expandEntry(Entity dataEntry, boolean replaceContext) throws JsonProcessingException,
                                                                                             NotFoundException;

    ExpandedOrganization getOrganization(Entity dataEntry) throws NotFoundException;

    ExpandedPerson expandPerson(User username);

    ExpandedMessage expandMessage(Message messages);
}
