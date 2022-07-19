package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.identifiers.SortableIdentifier;

import org.junit.jupiter.api.Test;

public class IdentifierEntryTest {
    
    public static final SortableIdentifier SAMPLE_IDENTIFIER = SortableIdentifier.next();
    
    @Test
    public void createReturnsIdentifierEntryWithNonEmptyIdentifier() {
        
        DynamoEntry dataEntry = new DynamoEntry() {
            @Override
            public SortableIdentifier getIdentifier() {
                return SAMPLE_IDENTIFIER;
            }
        };
        IdentifierEntry identifierEntry = IdentifierEntry.create(dataEntry);
        String expectedIdentifierPartitionKey = identifierEntry.getType()
                                                + KEY_FIELDS_DELIMITER
                                                + SAMPLE_IDENTIFIER.toString();
        assertThat(identifierEntry.getPrimaryKeyPartitionKey(), is(equalTo(expectedIdentifierPartitionKey)));
    }
    
}