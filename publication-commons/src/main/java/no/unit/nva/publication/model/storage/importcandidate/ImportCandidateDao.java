package no.unit.nva.publication.model.storage.importcandidate;

import static no.unit.nva.publication.storage.model.DatabaseConstants.IMPORT_CANDIDATE_KEY_PATTERN;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ImportCandidate")
public class ImportCandidateDao implements DatabaseEntryWithData<ImportCandidate> {

    @JsonProperty("data")
    private ImportCandidate data;
    @JsonProperty("identifier")
    private SortableIdentifier identifier;

    /**
     * No-arg constructor required by Jackson for deserialization.
     */
    @JsonCreator
    public ImportCandidateDao() {

    }

    public ImportCandidateDao(ImportCandidate data, SortableIdentifier identifier) {
        this.data = data.copy().withIdentifier(identifier).build();
        this.identifier = identifier;
    }

    @Override
    public ImportCandidate getData() {
        return data;
    }

    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getPartitionKey() {
        return IMPORT_CANDIDATE_KEY_PATTERN.formatted(identifier.toString());
    }

    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public String getSortKey() {
        return IMPORT_CANDIDATE_KEY_PATTERN.formatted(identifier.toString());
    }
}
