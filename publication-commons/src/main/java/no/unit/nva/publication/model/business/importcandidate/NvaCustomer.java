package no.unit.nva.publication.model.business.importcandidate;

import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public record NvaCustomer(boolean isCustomer, URI cristinId) implements JsonSerializable {

}