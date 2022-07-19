package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.WithIdentifier;
import org.junit.jupiter.api.Test;

public class IdentifierEntryTest {
    
    public static final SortableIdentifier SAMPLE_IDENTIFIER = SortableIdentifier.next();
    
    @Test
    public void createReturnsIdentifierEntryWithNonEmptyIdentifier() {
        
        DataEntry dataEntry = new DataEntry();
        IdentifierEntry identifierEntry = IdentifierEntry.create(dataEntry);
        String expectedIdentifierPartitionKey = identifierEntry.getType()
                                                + KEY_FIELDS_DELIMITER
                                                + SAMPLE_IDENTIFIER.toString();
        assertThat(identifierEntry.getPrimaryKeyPartitionKey(), is(equalTo(expectedIdentifierPartitionKey)));
    }
    
    private static class DataEntry implements WithIdentifier {
        
        @Override
        public SortableIdentifier getIdentifier() {
            return SAMPLE_IDENTIFIER;
        }
        
        @Override
        public void setIdentifier(SortableIdentifier identifier) {
        
        }
    }
}