package no.unit.nva.dataimport;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import java.util.Collection;
import java.util.stream.Collectors;

public class BatchInsertionFailureException extends RuntimeException {

    public static final String DELIMITER = System.lineSeparator();

    public BatchInsertionFailureException(BatchWriteItemResult result) {
        super(constructMessage(result));
    }

    private static String constructMessage(BatchWriteItemResult result) {
        return result.getUnprocessedItems().values()
                   .stream()
                   .flatMap(Collection::stream)
                   .map(WriteRequest::getPutRequest)
                   .map(PutRequest::getItem)
                   .map(BatchInsertionFailureException::identifyItem)
                   .collect(Collectors.joining(DELIMITER));
    }

    private static String identifyItem(java.util.Map<String, AttributeValue> item) {
        String primaryKeyPk = item.get(PRIMARY_KEY_PARTITION_KEY_NAME).getS();
        String primaryKeySk = item.get(PRIMARY_KEY_SORT_KEY_NAME).getS();
        return primaryKeyToString(primaryKeyPk, primaryKeySk);
    }

    private static String primaryKeyToString(String primaryKeyPk, String primaryKeySk) {
        return String.format("%s:%s,%s:%s",
                             PRIMARY_KEY_PARTITION_KEY_NAME,
                             primaryKeyPk,
                             PRIMARY_KEY_SORT_KEY_NAME,
                             primaryKeySk);
    }
}
