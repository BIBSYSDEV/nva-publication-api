package no.unit.nva.publication.model.storage.importcandidate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.storage.DynamoEntry;

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

    @JsonProperty("PK0")
    public String getPartitionKey() {
        return "ImportCandidate:%s".formatted(identifier.toString());
    }

    @JsonProperty("SK0")
    public String getSortKey() {
        return "ImportCandidate:%s".formatted(identifier.toString());
    }
}
