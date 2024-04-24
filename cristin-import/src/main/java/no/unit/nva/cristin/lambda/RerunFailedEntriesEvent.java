package no.unit.nva.cristin.lambda;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.paths.UnixPath;

public record RerunFailedEntriesEvent(UnixPath unixPath) implements JsonSerializable {

}
