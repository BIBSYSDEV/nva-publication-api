package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import nva.commons.core.JacocoGenerated;

public class QuerySpliterator implements Spliterator<Map<String, AttributeValue>> {
    
    private final QueryRequest queryRequest;
    private final int pageSize;
    private final AmazonDynamoDB client;
    private final Queue<Map<String, AttributeValue>> currentPage;
    private Map<String, AttributeValue> exclusiveStartKey;
    private boolean firstIteration = true;
    
    public QuerySpliterator(AmazonDynamoDB client, QueryRequest queryRequest, int pageSize) {
        this.queryRequest = queryRequest;
        this.pageSize = pageSize;
        this.client = client;
        this.currentPage = new LinkedList<>();
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super Map<String, AttributeValue>> action) {
        return fetchNextEntry().map(entry -> applyAction(action, entry)).orElse(false);
    }
    
    @JacocoGenerated
    @Override
    public Spliterator<Map<String, AttributeValue>> trySplit() {
        return null;
    }
    
    @JacocoGenerated
    @Override
    public long estimateSize() {
        return 0;
    }
    
    @Override
    public int characteristics() {
        return IMMUTABLE;
    }
    
    private boolean applyAction(Consumer<? super Map<String, AttributeValue>> action,
                                Map<String, AttributeValue> entry) {
        action.accept(entry);
        return true;
    }
    
    private Optional<Map<String, AttributeValue>> fetchNextEntry() {
        if (!currentPage.isEmpty()) {
            return Optional.of(currentPage.poll());
        }
        fillCurrentPageWithNewData();
        return Optional.ofNullable(currentPage.poll());
    }
    
    private void fillCurrentPageWithNewData() {
        var nextPage = fetchNextPage();
        currentPage.addAll(nextPage);
    }
    
    private List<Map<String, AttributeValue>> fetchNextPage() {
        if (thereAreMoreEntriesRemotely()) {
            var result = client.query(queryRequest.withLimit(pageSize).withExclusiveStartKey(exclusiveStartKey));
            var items = result.getItems();
            exclusiveStartKey = result.getLastEvaluatedKey();
            firstIteration = false;
            return nonNull(items) ? items : Collections.emptyList();
        }
        return Collections.emptyList();
    }
    
    private boolean thereAreMoreEntriesRemotely() {
        return nonNull(exclusiveStartKey) || firstIteration;
    }
}
