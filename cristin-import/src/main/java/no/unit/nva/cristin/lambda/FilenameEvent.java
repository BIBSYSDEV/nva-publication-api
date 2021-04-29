package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;

public class FilenameEvent implements JsonSerializable {

    @JsonProperty("fileUri")
    private final URI fileUri;

    @JsonCreator
    @JacocoGenerated
    public FilenameEvent(@JsonProperty("fileUri") String fileUri) {
        this.fileUri = Optional.ofNullable(fileUri).map(URI::create).orElseThrow();
    }

    public FilenameEvent(URI fileUri) {
        this.fileUri = fileUri;
    }

    public static FilenameEvent fromJson(String json) {
        return attempt(() -> JsonUtils.objectMapperWithEmpty.readValue(json, FilenameEvent.class)).orElseThrow();
    }

    public URI getFileUri() {
        return fileUri;
    }
}
