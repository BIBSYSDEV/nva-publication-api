package no.unit.nva.publication.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import no.unit.nva.publication.model.storage.CounterDao;

public class CristinIdentifierCounterService implements CounterService {

    public static final String UPDATING_COUNTER_EXCEPTION_MESSAGE = "Failed to update counter to value: ";
    private final AmazonDynamoDB client;

    protected CristinIdentifierCounterService(AmazonDynamoDB client) {
        this.client = client;
    }

    @Override
    public void insert(int value) {
        attempt(() -> CounterDao.fromValue(value))
            .map(CounterDao::toCreateTransactionWriteItem)
            .forEach(client::putItem)
            .orElseThrow();
    }

    @Override
    public CounterDao fetch() {
        return attempt(CounterDao::toGetItemRequest)
                   .map(client::getItem)
                   .map(CounterDao::fromGetItemResult)
                   .orElseThrow();
    }

    @Override
    public CounterDao increment() {
        var increasedCounter = CounterDao.fetch(this).increment();
        attempt(() -> CounterDao.fetch(this))
            .map(CounterDao::increment)
            .map(CounterDao::toPutTransactionWriteItem)
            .map(item -> new TransactWriteItemsRequest().withTransactItems(item))
            .forEach(client::transactWriteItems)
            .orElseThrow(failure -> new RuntimeException(UPDATING_COUNTER_EXCEPTION_MESSAGE + increasedCounter));
        return increasedCounter;
    }
}
