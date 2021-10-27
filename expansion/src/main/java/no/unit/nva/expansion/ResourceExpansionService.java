package no.unit.nva.expansion;

import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;

public interface ResourceExpansionService {

    ExpandedMessage expandMessage(Message message);

    ExpandedDoiRequest expandDoiRequest(DoiRequest doiRequest);

}
