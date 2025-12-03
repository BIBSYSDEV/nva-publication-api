package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.util.UUID;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.Test;

class ServiceWithTransactionsTest extends ResourcesLocalTest {

    @Test
    void newPutTransactionItemShouldFailWhenInsertingItemWithExistingIdentifier() {
        super.init();
        var dao = Resource.fromPublication(randomPublication()).toDao();

        sendTransaction(dao);

        assertThrows(RuntimeException.class, () -> sendTransaction(dao));
    }

    @Test
    void newPutTransactionItemWithLockingShouldSucceedWhenVersionMatches() {
        super.init();
        var dao = Resource.fromPublication(randomPublication()).toDao();
        var originalVersion = dao.getVersion();

        sendTransaction(dao);

        dao.setVersion(UUID.randomUUID());
        var transactItem = ServiceWithTransactions.newPutTransactionItemWithLocking(
            dao, originalVersion, RESOURCES_TABLE_NAME);
        var request = new TransactWriteItemsRequest().withTransactItems(transactItem);

        assertDoesNotThrow(() -> client.transactWriteItems(request));
    }

    @Test
    void newPutTransactionItemWithLockingShouldFailWhenVersionMismatch() {
        super.init();
        var dao = Resource.fromPublication(randomPublication()).toDao();

        sendTransaction(dao);

        var wrongVersion = UUID.randomUUID();
        var transactItem = ServiceWithTransactions.newPutTransactionItemWithLocking(
            dao, wrongVersion, RESOURCES_TABLE_NAME);
        var request = new TransactWriteItemsRequest().withTransactItems(transactItem);

        assertThrows(RuntimeException.class, () -> client.transactWriteItems(request));
    }

    private void sendTransaction(Dao dao) {
        var transaction = ServiceWithTransactions.newPutTransactionItem(dao, RESOURCES_TABLE_NAME);
        var request = new TransactWriteItemsRequest().withTransactItems(transaction);
        client.transactWriteItems(request);
    }
}