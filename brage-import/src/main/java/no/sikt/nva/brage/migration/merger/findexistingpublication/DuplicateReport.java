package no.sikt.nva.brage.migration.merger.findexistingpublication;

import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;

public record DuplicateReport(List<SortableIdentifier> duplicates,
                              DuplicateDetectionCause cause,
                              URI brageHandle) implements JsonSerializable {

}
