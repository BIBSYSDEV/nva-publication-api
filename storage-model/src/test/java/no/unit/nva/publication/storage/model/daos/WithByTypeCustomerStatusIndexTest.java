package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.STATUS_INDEX_FIELD_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import com.amazonaws.services.dynamodbv2.model.Condition;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.publication.storage.model.WithStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void fetchEntryCollectionByTypeCustomerStatusKeyReturnsConditionForFetchingEntryCollection(Dao<?> dao) {
        Map<String, Condition> condition =
            dao.fetchEntryCollectionByTypeCustomerStatusKey();
        assertThat(condition, hasKey(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME));
        Condition actualCondition = condition.get(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME);
        String actualValue = actualCondition.getAttributeValueList().get(0).getS();

        String expectedValue = constructExpectedPartitionKeyFormat(dao);

        assertThat(actualValue, is(equalTo(expectedValue)));
    }

    private static Stream<Dao<?>> instanceProvider() throws InvalidIssnException, MalformedURLException {
        return DaoUtils.instanceProvider();
    }

    private String constructExpectedPartitionKeyFormat(Dao<?> dao) {
        return dao.getType()
               + KEY_FIELDS_DELIMITER
               + CUSTOMER_INDEX_FIELD_PREFIX
               + KEY_FIELDS_DELIMITER
               + dao.getCustomerIdentifier()
               + KEY_FIELDS_DELIMITER
               + STATUS_INDEX_FIELD_PREFIX
               + KEY_FIELDS_DELIMITER
               + ((WithStatus) dao.getData()).getStatusString();
    }
}