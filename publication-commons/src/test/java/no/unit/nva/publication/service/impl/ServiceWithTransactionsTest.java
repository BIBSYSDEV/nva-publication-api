package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.Test;

class ServiceWithTransactionsTest extends ResourcesLocalTest {

    @Test
    void newPutTransactionItemShouldFailWhenInsertingItemWithExistingIdentifier() {
        super.init();
        var dao = Resource.fromPublication(randomPublication()).toDao();

        var service = new ServiceWithTransactions(super.client);

        sendTransaction(service, dao);

        assertThrows(RuntimeException.class, () -> sendTransaction(service, dao));

    }

    private void sendTransaction(ServiceWithTransactions service, Dao dao) {
        var transaction = service.newPutTransactionItem(dao, RESOURCES_TABLE_NAME);
        var request = new TransactWriteItemsRequest().withTransactItems(transaction);
        client.transactWriteItems(request);
    }
}