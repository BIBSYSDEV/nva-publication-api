package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import org.junit.jupiter.api.Test;

public class UniqueDoiRequestEntryTest {
    
    public static final SortableIdentifier SAMPLE_IDENTIFIER = SortableIdentifier.next();
    public static final SortableIdentifier RESOURCE_IDENTIFIER = SortableIdentifier.next();
    public static final URI SAMPLE_CUSTOMER_ID = URI.create("https://some.example.org/1234");
    
    @Test
    public void createReturnObjectWithPartitionKeyContainingTheInputEntryIdentifier() {
        
        DoiRequestDao dao = sampleDoiRequestDao();
        UniqueDoiRequestEntry uniqueDoiRequestEntry = UniqueDoiRequestEntry.create(dao);
        String expectedIdentifierPartitionKey = uniqueDoiRequestEntry.getType()
                                                + KEY_FIELDS_DELIMITER
                                                + RESOURCE_IDENTIFIER.toString();
        assertThat(uniqueDoiRequestEntry.getPrimaryKeyPartitionKey(), is(equalTo(expectedIdentifierPartitionKey)));
    }
    
    private DoiRequestDao sampleDoiRequestDao() {
        DoiRequest doiRequest = DoiRequest.builder()
            .withIdentifier(SAMPLE_IDENTIFIER)
            .withResourceIdentifier(RESOURCE_IDENTIFIER)
            .withCustomerId(SAMPLE_CUSTOMER_ID)
            .build();
        return new DoiRequestDao(doiRequest);
    }
}