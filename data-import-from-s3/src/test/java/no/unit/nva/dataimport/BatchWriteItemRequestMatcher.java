package no.unit.nva.dataimport;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import java.util.Collection;
import java.util.Optional;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Matcher for MockitoHamcrest to match arguments that contain a specific PartitionKey
 *
 * <p>Example Usage:
 * {@code when(mockClient.batchWriteItem(argThat(requestContains(failingPrimaryPartitionKey))))
 * .thenAnswer(answerForFailingPrimaryPartitionKey(failingPrimaryPartitionKey)); }
 */
public class BatchWriteItemRequestMatcher extends BaseMatcher<BatchWriteItemRequest> {

    private final String failingPk;

    public BatchWriteItemRequestMatcher(String failingPk) {
        this.failingPk = failingPk;
    }

    public static BatchWriteItemRequestMatcher requestContains(String failingPk) {
        return new BatchWriteItemRequestMatcher(failingPk);
    }

    @Override
    public boolean matches(Object actual) {
        BatchWriteItemRequest request = (BatchWriteItemRequest) actual;
        return Optional.ofNullable(request).stream()
                   .map(BatchWriteItemRequest::getRequestItems)
                   .flatMap(requestItems -> requestItems.values().stream())
                   .flatMap(Collection::stream)
                   .map(WriteRequest::getPutRequest)
                   .map(PutRequest::getItem)
                   .map(item -> item.get(PRIMARY_KEY_PARTITION_KEY_NAME))
                   .map(AttributeValue::getS).anyMatch(failingPk::equals);
    }

    @Override
    public void describeTo(Description description) {

    }
}
