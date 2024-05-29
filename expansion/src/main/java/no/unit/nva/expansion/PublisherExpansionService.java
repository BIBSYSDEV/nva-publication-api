package no.unit.nva.expansion;

import no.unit.nva.expansion.model.ExpandedPublisher;
import no.unit.nva.model.contexttypes.PublishingHouse;

public interface PublisherExpansionService {

    ExpandedPublisher createExpandedPublisher(PublishingHouse publishingHouse);

}
