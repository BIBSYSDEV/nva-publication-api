package no.unit.nva.cristin.utils;

import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;

public record NvaCustomerResponse(URI id) implements JsonSerializable {

}
