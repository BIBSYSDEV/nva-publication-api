package no.unit.nva.publication.events.handlers.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JacocoGenerated;

public class PersistedDocumentConsumptionAttributes {
    
    public static final String RESOURCES_INDEX = "resources";
    public static final String TICKETS_INDEX = "tickets";
    public static final String IMPORT_CANDIDATES_INDEX = "import-candidates";
    public static final String INDEX_FIELD = "index";
    public static final String DOCUMENT_IDENTIFIER = "documentIdentifier";
    public static final String UNSUPPORTED_TYPE_ERROR_MESSAGE = "Currently unsupported type of entry:";
    @JsonProperty(INDEX_FIELD)
    private final String index;
    @JsonProperty(DOCUMENT_IDENTIFIER)
    private final SortableIdentifier documentIdentifier;
    
    @JsonCreator
    public PersistedDocumentConsumptionAttributes(
        @JsonProperty(INDEX_FIELD) String index,
        @JsonProperty(DOCUMENT_IDENTIFIER) SortableIdentifier documentIdentifier
    ) {
        this.index = index;
        this.documentIdentifier = documentIdentifier;
    }
    
    public static PersistedDocumentConsumptionAttributes createAttributes(ExpandedDataEntry expandedEntry) {
        if (expandedEntry instanceof ExpandedResource) {
            return new PersistedDocumentConsumptionAttributes(RESOURCES_INDEX, expandedEntry.identifyExpandedEntry());
        } else if (expandedEntry instanceof ExpandedImportCandidate) {
            return new PersistedDocumentConsumptionAttributes(IMPORT_CANDIDATES_INDEX,
                                                              expandedEntry.identifyExpandedEntry());
        } else if (expandedEntry instanceof ExpandedTicket) {
            return new PersistedDocumentConsumptionAttributes(TICKETS_INDEX, expandedEntry.identifyExpandedEntry());
        }
        throw new UnsupportedOperationException(
            UNSUPPORTED_TYPE_ERROR_MESSAGE + expandedEntry.getClass().getSimpleName());
    }
    
    public SortableIdentifier getDocumentIdentifier() {
        return documentIdentifier;
    }
    
    public String getIndex() {
        return index;
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIndex(), getDocumentIdentifier());
    }
    
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistedDocumentConsumptionAttributes)) {
            return false;
        }
        PersistedDocumentConsumptionAttributes that = (PersistedDocumentConsumptionAttributes) o;
        return Objects.equals(getIndex(), that.getIndex()) && Objects.equals(getDocumentIdentifier(),
            that.getDocumentIdentifier());
    }
}
