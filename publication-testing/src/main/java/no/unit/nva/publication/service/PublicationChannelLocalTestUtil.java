package no.unit.nva.publication.service;

import java.util.List;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

public class PublicationChannelLocalTestUtil extends ResourcesLocalTest {

    public PublicationChannelLocalTestUtil() {
        super();
    }

    public void persistPublicationChannel(PublicationChannel publicationChannel) {
        var transaction = publicationChannel.toDao().toPutNewTransactionItem(DatabaseConstants.RESOURCES_TABLE_NAME);
        var request = TransactWriteItemsRequest.builder().transactItems(List.of(transaction)).build();
        client.transactWriteItems(request);
    }
}
