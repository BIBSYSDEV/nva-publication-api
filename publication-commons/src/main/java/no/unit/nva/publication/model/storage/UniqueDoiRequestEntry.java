package no.unit.nva.publication.model.storage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import nva.commons.core.JacocoGenerated;

@JsonTypeName("DoiRequestEntry")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UniqueDoiRequestEntry extends UniquenessEntry {
    
    private static final String TYPE = "DoiRequestEntry";
    
    @JacocoGenerated
    public UniqueDoiRequestEntry() {
        super();
    }
    
    public UniqueDoiRequestEntry(String identifier) {
        super(identifier);
    }
    
    public static UniqueDoiRequestEntry create(DoiRequestDao doiRequestDao) {
        return new UniqueDoiRequestEntry(doiRequestDao.getResourceIdentifier().toString());
    }
    
    @Override
    protected String getType() {
        return TYPE;
    }
}
