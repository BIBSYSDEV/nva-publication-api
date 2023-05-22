package no.unit.nva.publication.events.bodies;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.ImportCandidate;

public class ImportCandidateDataEntryUpdate implements JsonSerializable {

    public static final String IMPORT_CANDIDATE_UPDATE = "ImportCandidates.DataEntry.Update";
    public static final String IMPORT_CANDIDATE_DELETION = "ImportCandidates.DataEntry.Delete";
    public static final String IMPORT_CANDIDATE_PERSISTENCE = "ImportCandidates.ExpandedDataEntry.Persisted";
    private static final String ACTION = "action";
    private static final String OLD_DATA = "oldData";
    private static final String NEW_DATA = "newData";
    @JsonProperty(ACTION)
    private final String action;
    @JsonProperty(OLD_DATA)
    private final ImportCandidate oldData;
    @JsonProperty(NEW_DATA)
    private final ImportCandidate newData;

    @JsonCreator
    public ImportCandidateDataEntryUpdate(
        @JsonProperty(ACTION) String action,
        @JsonProperty(OLD_DATA) ImportCandidate oldData,
        @JsonProperty(NEW_DATA) ImportCandidate newData) {

        this.action = action;
        this.oldData = oldData;
        this.newData = newData;
    }

    public static ImportCandidateDataEntryUpdate fromJson(String json) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, ImportCandidateDataEntryUpdate.class))
                   .orElseThrow();
    }

    public String getAction() {
        return action;
    }

    public ImportCandidate getOldData() {
        return oldData;
    }

    public ImportCandidate getNewData() {
        return newData;
    }

    @JsonIgnore
    public boolean notEmpty() {
        return nonNull(oldData) || nonNull(newData);
    }

    @JsonProperty("topic")
    public String getTopic() {
        return isNull(newData)
                   ? IMPORT_CANDIDATE_DELETION
                   : IMPORT_CANDIDATE_UPDATE;
    }
}
