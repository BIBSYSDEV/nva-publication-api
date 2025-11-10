package no.unit.nva.publication.events.bodies;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import nva.commons.core.JacocoGenerated;

public class ImportCandidateDataEntryUpdate implements JsonSerializable {

    public static final String IMPORT_CANDIDATE_UPDATE = "ImportCandidates.DataEntry.Update";
    public static final String IMPORT_CANDIDATE_DELETION = "ImportCandidates.DataEntry.Delete";
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
    public ImportCandidateDataEntryUpdate(@JsonProperty(ACTION) String action,
                                          @JsonProperty(OLD_DATA) ImportCandidate oldData,
                                          @JsonProperty(NEW_DATA) ImportCandidate newData) {

        this.action = action;
        this.oldData = oldData;
        this.newData = newData;
    }

    public static ImportCandidateDataEntryUpdate fromJson(String json) {
        return attempt(
            () -> JsonUtils.dtoObjectMapper.readValue(json, ImportCandidateDataEntryUpdate.class)).orElseThrow();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getAction(), getOldData(), getNewData());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImportCandidateDataEntryUpdate that)) {
            return false;
        }
        return Objects.equals(getAction(), that.getAction()) && Objects.equals(getOldData(), that.getOldData()) &&
               Objects.equals(getNewData(), that.getNewData());
    }

    public String getAction() {
        return action;
    }

    public Optional<ImportCandidate> getOldData() {
        return Optional.ofNullable(oldData);
    }

    public Optional<ImportCandidate> getNewData() {
        return Optional.ofNullable(newData);
    }

    @JsonProperty("topic")
    public String getTopic() {
        return isNull(newData) ? IMPORT_CANDIDATE_DELETION : IMPORT_CANDIDATE_UPDATE;
    }
}
