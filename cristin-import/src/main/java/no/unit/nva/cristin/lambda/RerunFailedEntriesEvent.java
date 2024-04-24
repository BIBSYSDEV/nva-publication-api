package no.unit.nva.cristin.lambda;

import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;

public record RerunFailedEntriesEvent(URI uri) implements JsonSerializable {

    public UnixPath s3Path() {
        return UriWrapper.fromUri(uri).getPath();
    }
}
