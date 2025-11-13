package no.unit.nva.publication.model.storage.importcandidate;

import static no.unit.nva.publication.model.storage.DataCompressor.compress;
import static no.unit.nva.publication.storage.model.DatabaseConstants.IMPORT_CANDIDATE_KEY_PATTERN;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import nva.commons.core.JacocoGenerated;

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

    @JsonIgnore
    public Map<String, AttributeValue> toDynamoFormat() {
        return attempt(() -> compress(this)).orElseThrow();
    }

    @Override
    public ImportCandidate getData() {
        return data;
    }

    @JacocoGenerated
    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
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
