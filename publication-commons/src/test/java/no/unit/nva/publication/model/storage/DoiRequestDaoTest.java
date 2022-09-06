package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_INDEX_FIELD_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.UserInstance;
import org.junit.jupiter.api.Test;

class DoiRequestDaoTest {
    
    private static final String SAMPLE_USER = "some@onwer";
    private static final String SAMPLE_PUBLISHER_IDENTIFIER = "somePublsherId";
    private static final URI SAMPLE_PUBLISHER = URI.create("https://some.example.org/" + SAMPLE_PUBLISHER_IDENTIFIER);
    private static final UserInstance SAMPLE_USER_INSTANCE = UserInstance.create(SAMPLE_USER, SAMPLE_PUBLISHER);
    private static final SortableIdentifier SAMPLE_RESOURCE_IDENTIFIER = SortableIdentifier.next();
    
    @Test
    public void queryByCustomerAndResourceIdentifierReturnsObjectWithPartitionKeyContainingPublisherAndResourceId() {
        
        DoiRequestDao queryObject =
            DoiRequestDao.queryByCustomerAndResourceIdentifier(SAMPLE_USER_INSTANCE, SAMPLE_RESOURCE_IDENTIFIER);
        String expectedPartitionKey = CUSTOMER_INDEX_FIELD_PREFIX
                                      + KEY_FIELDS_DELIMITER
                                      + SAMPLE_PUBLISHER_IDENTIFIER
                                      + KEY_FIELDS_DELIMITER
                                      + RESOURCE_INDEX_FIELD_PREFIX
                                      + KEY_FIELDS_DELIMITER
                                      + SAMPLE_RESOURCE_IDENTIFIER;
        
        assertThat(queryObject.getByCustomerAndResourcePartitionKey(), is(equalTo(expectedPartitionKey)));
    }
    
    @Test
    public void queryObjectWithOwnerAndResourceReturnsQueryObjectEnablingRetrievalOfAllDoiRequestsOfUser() {
        DoiRequestDao queryObject = DoiRequestDao.queryObject(SAMPLE_PUBLISHER, SAMPLE_USER);
        String expectedPrimaryPartitionKey = expectedDoiRequestPrimaryPartitionKey();
        assertThat(queryObject.getPrimaryKeyPartitionKey(), is(equalTo(expectedPrimaryPartitionKey)));
    }
    
    @Test
    public void queryObjectWithOwnerResourceAndEntryIdentifierReturnsQueryObjectWithCompletePrimaryKey() {
        SortableIdentifier sampleEntryIdentifier = SortableIdentifier.next();
        DoiRequestDao queryObject = DoiRequestDao.queryObject(SAMPLE_PUBLISHER, SAMPLE_USER, sampleEntryIdentifier);
        
        assertThat(queryObject.getPrimaryKeyPartitionKey(), is(equalTo(expectedDoiRequestPrimaryPartitionKey())));
        assertThat(queryObject.getPrimaryKeySortKey(),
            is(equalTo(expectedDoiRequestPrimarySortKey(sampleEntryIdentifier))));
    }
    
    private String expectedDoiRequestPrimarySortKey(SortableIdentifier entryIdentifier) {
        return TicketDao.TICKETS_INDEXING_TYPE
               + KEY_FIELDS_DELIMITER
               + entryIdentifier.toString();
    }
    
    private String expectedDoiRequestPrimaryPartitionKey() {
        return TicketDao.TICKETS_INDEXING_TYPE
               + KEY_FIELDS_DELIMITER
               + SAMPLE_PUBLISHER_IDENTIFIER
               + KEY_FIELDS_DELIMITER
               + SAMPLE_USER;
    }
}