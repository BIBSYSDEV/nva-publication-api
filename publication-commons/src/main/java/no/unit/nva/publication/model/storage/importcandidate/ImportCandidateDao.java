package no.unit.nva.publication.model.storage.importcandidate;

import static no.unit.nva.publication.storage.model.DatabaseConstants.IMPORT_CANDIDATE_KEY_PATTERN;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.storage.DynamoEntry;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ImportCandidate")
public class ImportCandidateDao implements DynamoEntry {

    private ImportCandidate importCandidate;
    private SortableIdentifier identifier;

    /**
     * No-arg constructor required by Jackson for deserialization.
     */
    @JsonCreator
    public ImportCandidateDao() {

    }

    public ImportCandidateDao(ImportCandidate importCandidate, SortableIdentifier identifier) {
        this.importCandidate = importCandidate.copy().withIdentifier(identifier).build();
        this.identifier = identifier;
    }

    public ImportCandidate getImportCandidate() {
        return importCandidate;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return importCandidate.getIdentifier();
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
