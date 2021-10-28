package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.IndexDocument;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;

public interface ResourceExpansionService {

    ExpandedMessage expandMessage(Message message);

    ExpandedDoiRequest expandDoiRequest(DoiRequest doiRequest);

    IndexDocument expandResource(Resource resource) throws JsonProcessingException;
}
