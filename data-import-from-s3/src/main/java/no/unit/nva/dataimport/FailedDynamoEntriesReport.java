package no.unit.nva.dataimport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JsonSerializable;

public class FailedDynamoEntriesReport implements JsonSerializable {

    public static final String ENTRY_KEY_FIELD = "entryKeys";
    public static final String INPUT_FILENAME = "inputFilename";
    @JsonProperty(ENTRY_KEY_FIELD)
    private final String entryKeys;
    @JsonProperty(INPUT_FILENAME)
    private final String inputFilename;

    @JsonCreator
    public FailedDynamoEntriesReport(@JsonProperty(ENTRY_KEY_FIELD) String entryKeys,
                                     @JsonProperty(INPUT_FILENAME) String inputFilename) {
        this.entryKeys = entryKeys;
        this.inputFilename = inputFilename;
    }

    public String getEntryKeys() {
        return entryKeys;
    }

    public String getInputFilename() {
        return inputFilename;
    }
}
