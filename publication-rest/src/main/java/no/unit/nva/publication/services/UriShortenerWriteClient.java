package no.unit.nva.publication.services;

import static no.unit.nva.publication.services.storage.UriMapDao.URI_MAP_PRIMARY_PARTITION_KEY;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.services.model.UriMap;
import no.unit.nva.publication.services.storage.UriMapDao;
import nva.commons.core.attempt.Failure;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

public class UriShortenerWriteClient {


    public static final String PARTITION_KEY_NAME_PLACEHOLDER = "#partitionKey";
    public static final String KEY_NOT_EXISTS_CONDITION = keyNotExistsCondition();
    public static final Map<String, String> PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES =
        primaryKeyEqualityConditionAttributeNames();
    private final DynamoDbClient client;
    private final String tableName;

    public UriShortenerWriteClient(DynamoDbClient client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    public void insertUriMap(UriMap uriMap) {
        var transactWriteItemsRequest = newPutTransactWriteItemsRequest(new UriMapDao(uriMap));
        sendTransactionWriteRequest(transactWriteItemsRequest);
    }

    private TransactWriteItemsRequest newPutTransactWriteItemsRequest(UriMapDao data) {
        var transactWriteItem = newPutTransactionItem(data);
        return newTransactWriteItemsRequest(transactWriteItem);
    }

    private static TransactWriteItemsRequest newTransactWriteItemsRequest(TransactWriteItem... transaction) {
        return newTransactWriteItemsRequest(Arrays.asList(transaction));
    }

    private static TransactWriteItemsRequest newTransactWriteItemsRequest(List<TransactWriteItem> transactionItems) {
        return TransactWriteItemsRequest.builder().transactItems(transactionItems).build();
    }

    private TransactWriteItem newPutTransactionItem(UriMapDao data) {
        var put = Put.builder()
                      .item(data.toDynamoFormat())
                      .tableName(tableName)
                      .conditionExpression(KEY_NOT_EXISTS_CONDITION)
                      .expressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                      .build();
        return TransactWriteItem.builder().put(put).build();
    }

    private void sendTransactionWriteRequest(TransactWriteItemsRequest transactWriteItemsRequest) {
        attempt(() -> client.transactWriteItems(transactWriteItemsRequest))
            .orElseThrow(this::handleTransactionFailure);
    }

    private static String keyNotExistsCondition() {
        return String.format("attribute_not_exists(%s)",
                             PARTITION_KEY_NAME_PLACEHOLDER);
    }

    private TransactionFailedException handleTransactionFailure(Failure<TransactWriteItemsResponse> fail) {
        return new TransactionFailedException(fail.getException());
    }

    private static Map<String, String> primaryKeyEqualityConditionAttributeNames() {
        return Map.of(
            PARTITION_KEY_NAME_PLACEHOLDER, URI_MAP_PRIMARY_PARTITION_KEY
        );
    }
}
