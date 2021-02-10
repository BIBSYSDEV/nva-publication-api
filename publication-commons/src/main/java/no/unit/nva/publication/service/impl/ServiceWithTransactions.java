package no.unit.nva.publication.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.attempt.Failure;

public abstract class ServiceWithTransactions {

    protected abstract String getTableName();

    protected abstract AmazonDynamoDB getClient();

    protected TransactWriteItem createTransactionEntryForInsertingResource(Resource resource) {
        return createTransactionPutEntry(new ResourceDao(resource));
    }

    protected <T extends WithPrimaryKey> TransactWriteItem createTransactionPutEntry(T resourceDao) {
        return ResourceServiceUtils.createTransactionPutEntry(resourceDao, getTableName());
    }

    protected <T extends WithPrimaryKey> TransactWriteItem newDeleteTransactionItem(Resource resource) {
        ResourceDao resourceDao = new ResourceDao(resource);
        return new TransactWriteItem()
            .withDelete(new Delete().withTableName(getTableName()).withKey(resourceDao.primaryKey()));
    }

    protected TransactWriteItemsResult sendTransactionWriteRequest(TransactWriteItemsRequest transactWriteItemsRequest)
        throws ConflictException {
        return attempt(() -> getClient().transactWriteItems(transactWriteItemsRequest))
            .orElseThrow(this::handleTransactionFailure);
    }

    private ConflictException handleTransactionFailure(Failure<TransactWriteItemsResult> fail) {
        return new ConflictException(fail.getException());
    }
}
