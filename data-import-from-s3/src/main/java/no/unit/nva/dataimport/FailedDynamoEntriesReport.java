package no.unit.nva.dataimport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.s3.UnixPath;
import nva.commons.core.JsonSerializable;

public class FailedDynamoEntriesReport implements JsonSerializable {

    public static final String ENTRY_KEY_FIELD = "entryKeys";
    public static final String INPUT_FILENAME = "inputFilename";
    @JsonProperty(ENTRY_KEY_FIELD)
    private final String entryKeys;
    @JsonProperty(INPUT_FILENAME)
    private final UnixPath inputFilePath;

    @JsonCreator
    public FailedDynamoEntriesReport(@JsonProperty(ENTRY_KEY_FIELD) String entryKeys,
                                     @JsonProperty(INPUT_FILENAME) UnixPath inputFilename) {
        this.entryKeys = entryKeys;
        this.inputFilePath = inputFilename;
    }

    public String getEntryKeys() {
        return entryKeys;
    }

    public UnixPath getInputFilePath() {
        return inputFilePath;
    }
}
