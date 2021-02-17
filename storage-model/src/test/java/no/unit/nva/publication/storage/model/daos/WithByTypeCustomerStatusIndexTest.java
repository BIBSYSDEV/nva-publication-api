package no.unit.nva.publication.storage.model.daos;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class WithByTypeCustomerStatusIndexTest {
    
    public static final String SAMPLE_TYPE = "sampleType";
    public static final String SAMPLE_STATUS = "sampleStatus";
    public static final String SAMPLE_CUSTOMER_IDENTIFIER = "123";
    public static final URI SAMPLE_CUSTOMER_ID = URI.create("https://example.org/" + SAMPLE_CUSTOMER_IDENTIFIER);
    
    @Test
    public void formatByTypeCustomerStatusPartitionKeyReturnsKeyEnablingSearchByCustomerAndStatus() {
        String partitionKey = WithByTypeCustomerStatusIndex.formatByTypeCustomerStatusPartitionKey(
            SAMPLE_TYPE,
            SAMPLE_STATUS,
            SAMPLE_CUSTOMER_ID
        );
        
        assertThat(partitionKey, containsString(SAMPLE_TYPE));
        assertThat(partitionKey, containsString(SAMPLE_STATUS));
        assertThat(partitionKey, containsString(SAMPLE_CUSTOMER_IDENTIFIER));
    }
}