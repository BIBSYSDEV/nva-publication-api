package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.publication.storage.model.WithIdentifier;
import nva.commons.core.JacocoGenerated;

@JsonTypeName("IdEntry")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class IdentifierEntry extends UniquenessEntry {
    
    private static final String TYPE = "IdEntry";
    
    /*For JSON Jackson*/
    @JacocoGenerated
    public IdentifierEntry() {
        super();
    }
    
    public IdentifierEntry(String identifier) {
        super(identifier);
    }
    
    public static IdentifierEntry create(WithIdentifier withIdentifier) {
        return new IdentifierEntry(withIdentifier.getIdentifier().toString());
    }
    
    @Override
    protected String getType() {
        return TYPE;
    }
}
