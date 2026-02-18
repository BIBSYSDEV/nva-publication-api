package no.unit.nva.publication.s3imports;

import static java.util.Objects.requireNonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.io.InputStream;
import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.ioutils.IoUtils;

public record UpdatePublicationsFromBrageRequest(URI uri, String archive, boolean dryRun, UpdateType type,
                                                 String value) implements JsonSerializable {

    public UpdatePublicationsFromBrageRequest {
        requireNonNull(uri, "Uri cannot be missing");
        requireNonNull(archive, "Archive cannot be missing");
    }

    public static UpdatePublicationsFromBrageRequest fromInputStream(InputStream inputStream) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(IoUtils.streamToString(inputStream),
                                                                 UpdatePublicationsFromBrageRequest.class)).orElseThrow();
    }

    public enum UpdateType {
        ABSTRACT, AFFILIATION
    }
}