package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.LogEntryDao.KEY_PATTERN;
import static no.unit.nva.publication.model.storage.TicketDao.newPutTransactionItem;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import nva.commons.core.JacocoGenerated;

public class PublicationChannelDao extends Dao implements DynamoEntryByIdentifier {

    protected static final String TYPE = "PublicationChannel";
    protected static final String IDENTIFIER = "identifier";
    protected static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    protected static final String DATA = "data";
    private final SortableIdentifier identifier;
    private final SortableIdentifier resourceIdentifier;

    @JsonCreator
    public PublicationChannelDao(@JsonProperty(IDENTIFIER) SortableIdentifier identifier,
                                 @JsonProperty(RESOURCE_IDENTIFIER) SortableIdentifier resourceIdentifier,
                                 @JsonProperty(DATA) PublicationChannel publicationChannel) {
        super(publicationChannel);
        this.identifier = identifier;
        this.resourceIdentifier = resourceIdentifier;
    }

    public static PublicationChannelDao fromPublicationChannel(PublicationChannel publicationChannel) {
        return new PublicationChannelDao(publicationChannel.getIdentifier(), publicationChannel.getResourceIdentifier(),
                                         publicationChannel);
    }

    @Override
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getPrimaryKeyPartitionKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
    }

    @Override
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public String getPrimaryKeySortKey() {
        return KEY_PATTERN.formatted(Resource.TYPE, getResourceIdentifier());
    }

    //TODO: Remove JacocoGenerated annotation once method is in use
    @JacocoGenerated
    @Override
    public String indexingType() {
        return TYPE;
    }

    @JacocoGenerated
    @Override
    public URI getCustomerId() {
        return null;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    //TODO: Remove JacocoGenerated annotation once method is in use
    @JacocoGenerated
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        return new TransactWriteItemsRequest().withTransactItems(newPutTransactionItem(this));
    }

    @JacocoGenerated
    @Override
    public void updateExistingEntry(AmazonDynamoDB client) {
        throw new UnsupportedOperationException();
    }

    @JacocoGenerated
    @Override
    protected User getOwner() {
        return null;
    }

    @Override
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    public String getByTypeAndIdentifierPartitionKey() {
        return KEY_PATTERN.formatted(Resource.TYPE, getResourceIdentifier());
    }

    @Override
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
    public String getByTypeAndIdentifierSortKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }
}
