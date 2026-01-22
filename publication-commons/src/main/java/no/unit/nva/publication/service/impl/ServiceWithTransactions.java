package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.model.storage.JoinWithResource.Constants.RESOURCE_INDEX_IN_QUERY_RESULT;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceService.AWAIT_TIME_BEFORE_FETCH_RETRY;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.WithPrimaryKey;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.FunctionWithException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

public class ServiceWithTransactions {

    public static final String EMPTY_STRING = "";
    public static final String STATUS_FIELD_IN_RESOURCE = "status";
    private static final Integer MAX_FETCH_ATTEMPTS = 3;
    private static final int TRANSACTION_BATCH_SIZE = 100;
    private static final String VERSION_FIELD = "version";
    private static final String VERSION_CONDITION_EXPRESSION = "#version = :expectedVersion";
    private static final String VERSION_ATTRIBUTE_NAME = "#version";
    private static final String EXPECTED_VERSION_VALUE = ":expectedVersion";

    protected final DynamoDbClient client;

    protected ServiceWithTransactions(DynamoDbClient client) {
        this.client = client;
    }

    protected static <T extends DynamoEntry> TransactWriteItem newPutTransactionItem(T data, String tableName) {
        var put = newPut(data, tableName);
        return TransactWriteItem.builder().put(put).build();
    }

    /**
     * Creates a DynamoDB Put operation for inserting a new entry.
     *
     * <p>The operation includes a condition expression that ensures the item does not
     * already exist in the table (based on primary key). This prevents accidental
     * overwrites of existing entries.
     *
     * @param data the DynamoEntry to insert into DynamoDB
     * @param tableName the name of the DynamoDB table
     * @return a Put operation configured with the entry data, table name, and a
     *         conditional expression that fails if the primary key already exists.
     */
    protected static <T extends DynamoEntry> Put newPut(T data, String tableName) {
        return Put.builder()
                   .item(data.toDynamoFormat())
                   .tableName(tableName)
                   .conditionExpression(KEY_NOT_EXISTS_CONDITION)
                   .expressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                   .build();
    }

    protected static <T extends Dao> TransactWriteItem newPutTransactionItemWithLocking(
        T dao, UUID expectedVersion, String tableName) {
        var put = Put.builder()
                      .tableName(tableName)
                      .item(dao.toDynamoFormat())
                      .conditionExpression(VERSION_CONDITION_EXPRESSION)
                      .expressionAttributeNames(Map.of(VERSION_ATTRIBUTE_NAME, VERSION_FIELD))
                      .expressionAttributeValues(Map.of(
                          EXPECTED_VERSION_VALUE,
                          AttributeValue.builder().s(expectedVersion.toString()).build()))
                      .build();
        return TransactWriteItem.builder().put(put).build();
    }

    protected static TransactWriteItemsRequest newTransactWriteItemsRequest(TransactWriteItem... transaction) {
        return newTransactWriteItemsRequest(Arrays.asList(transaction));
    }

    protected static TransactWriteItemsRequest newTransactWriteItemsRequest(List<TransactWriteItem> transactionItems) {
        return TransactWriteItemsRequest.builder().transactItems(transactionItems).build();
    }

    protected <T extends Entity, E extends Exception> Optional<T> fetchEventualConsistentDataEntry(
        T dynamoEntry,
        FunctionWithException<T, T, E> nonEventuallyConsistentFetch) {
        T savedEntry = null;
        for (int times = 0; times < MAX_FETCH_ATTEMPTS && savedEntry == null; times++) {
            savedEntry = attempt(() -> nonEventuallyConsistentFetch.apply(dynamoEntry)).orElse(fail -> null);
            attempt(this::waitBeforeFetching).orElseThrow();
        }
        return Optional.ofNullable(savedEntry);
    }

    protected final DynamoDbClient getClient() {
        return client;
    }

    protected <T extends WithPrimaryKey> TransactWriteItem newDeleteTransactionItem(T dynamoEntry) {
        return TransactWriteItem.builder()
                   .delete(Delete.builder()
                               .tableName(RESOURCES_TABLE_NAME)
                               .key(dynamoEntry.primaryKey())
                               .build())
                   .build();
    }

    protected ResourceDao extractResourceDao(List<Dao> daos) throws BadRequestException {
        if (doiRequestExists(daos) || onlyResourceExists(daos)) {
            return (ResourceDao) daos.get(RESOURCE_INDEX_IN_QUERY_RESULT);
        }
        throw new BadRequestException(RESOURCE_NOT_FOUND_MESSAGE);
    }

    protected void sendTransactionWriteRequest(TransactWriteItemsRequest transactWriteItemsRequest) {
        var counter = new AtomicInteger();

        transactWriteItemsRequest.transactItems()
            .stream()
            .collect(Collectors.groupingBy(
                it -> counter.getAndIncrement() / TRANSACTION_BATCH_SIZE
            ))
            .values()
            .forEach(batch -> {
                var batchRequest = TransactWriteItemsRequest.builder()
                    .transactItems(batch)
                    .build();
                attempt(() -> getClient().transactWriteItems(batchRequest)).orElseThrow(this::handleTransactionFailure);
            });
    }

    @SuppressWarnings("PMD.DoNotUseThreads")
    private Void waitBeforeFetching() throws InterruptedException {
        Thread.sleep(AWAIT_TIME_BEFORE_FETCH_RETRY);
        return null;
    }

    private TransactionFailedException handleTransactionFailure(Failure<TransactWriteItemsResponse> fail) {
        return new TransactionFailedException(fail.getException());
    }

    private boolean onlyResourceExists(List<Dao> daos) {
        return daos.size() == 1;
    }

    private boolean doiRequestExists(List<Dao> daos) {
        return daos.size() == 2;
    }
}
