package no.unit.nva.publication.service;

import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.util.List;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.storage.model.DatabaseConstants;

public class PublicationChannelLocalTestUtil extends ResourcesLocalTest {

    public PublicationChannelLocalTestUtil() {
        super();
    }

    public void persistPublicationChannel(PublicationChannel publicationChannel) {
        var transaction = publicationChannel.toDao().toPutNewTransactionItem(DatabaseConstants.RESOURCES_TABLE_NAME);
        var request = new TransactWriteItemsRequest().withTransactItems(List.of(transaction));
        client.transactWriteItems(request);
    }
}