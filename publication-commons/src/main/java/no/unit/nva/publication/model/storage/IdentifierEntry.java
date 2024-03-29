package no.unit.nva.publication.model.storage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
    
    public IdentifierEntry(DynamoEntry dynamoEntry) {
        this(dynamoEntry.getIdentifier().toString());
    }
    
    public static IdentifierEntry create(DynamoEntry dynamoEntry) {
        return new IdentifierEntry(dynamoEntry.getIdentifier().toString());
    }
    
    @Override
    protected String getType() {
        return TYPE;
    }
}
