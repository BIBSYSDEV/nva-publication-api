package no.unit.nva.dataimport;

import static java.lang.Math.min;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.s3.S3Driver;

public class S3ToDynamoImporter {

    public static final int BATCH_REQUEST_SIZE = 20;
    public static final int MAX_BATCH_INSERTION_ATTEMPTS = 10;

    public static final boolean NO_UNPROCESSED_ITEMS = false;

    private final AmazonDynamoDB dynamoClient;
    private final S3Reader s3Reader;
    private final String tableName;
    private final String filename;

    public S3ToDynamoImporter(AmazonDynamoDB dynamoClient,
                              S3Driver s3Driver,
                              String tableName,
                              String filename
    ) {
        this.dynamoClient = dynamoClient;
        this.s3Reader = new S3Reader(s3Driver);
        this.tableName = tableName;
        this.filename = filename;
    }

    public List<BatchWriteItemResult> insertFileToDynamo() throws IOException {
        List<Item> itemList = s3Reader.extractItemsFromS3Bucket(filename);
        return writeFileToDynamo(itemList, tableName);
    }

    private List<BatchWriteItemResult> writeFileToDynamo(List<Item> itemList, String tableName) {
        List<BatchWriteItemResult> results = new ArrayList<>();
        for (int i = 0; i < itemList.size(); i += BATCH_REQUEST_SIZE) {
            List<Item> batch = itemList.subList(i, min(i + BATCH_REQUEST_SIZE, itemList.size()));
            BatchWriteItemResult result = writeBatchToDynamo(batch, tableName);
            results.add(result);
        }
        return results;
    }

    private BatchWriteItemResult writeBatchToDynamo(List<Item> sublist, String tableName) {
        Map<String, List<WriteRequest>> writeRequests =
            sublist.stream().map(this::writeRequest)
                .collect(Collectors.groupingBy(allEntriesSavedToSameTable(tableName)));

        BatchWriteItemResult response = sendRequestsToDynamo(writeRequests);
        int attempts = 0;
        while (responseHasUnprocessedItems(response) && attempts < MAX_BATCH_INSERTION_ATTEMPTS) {
            response = sendRequestsToDynamo(response.getUnprocessedItems());
            attempts++;
        }
        return response;
    }

    private BatchWriteItemResult sendRequestsToDynamo(Map<String, List<WriteRequest>> writeRequests) {
        BatchWriteItemRequest batchRequest = new BatchWriteItemRequest().withRequestItems(writeRequests);
        return dynamoClient.batchWriteItem(batchRequest);
    }

    private boolean responseHasUnprocessedItems(BatchWriteItemResult response) {
        return Optional.ofNullable(response)
                   .map(BatchWriteItemResult::getUnprocessedItems)
                   .map(unprocessedItems -> !unprocessedItems.isEmpty())
                   .orElse(NO_UNPROCESSED_ITEMS);
    }

    private Function<WriteRequest, String> allEntriesSavedToSameTable(String tableName) {
        return item -> tableName;
    }

    private WriteRequest writeRequest(Item item) {
        PutRequest putRequest = new PutRequest().withItem(ItemUtils.toAttributeValues(item));
        return new WriteRequest().withPutRequest(putRequest);
    }
}
