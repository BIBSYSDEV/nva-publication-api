package no.unit.nva.publication.service;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_1_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_1_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_1_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.publication.TestDataSource;
import no.unit.nva.publication.external.services.ChannelClaimClient;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.CouplingBetweenObjects"})
@JacocoGenerated
public class ResourcesLocalTest extends TestDataSource {

    public static final ScalarAttributeType STRING_TYPE = ScalarAttributeType.S;
    protected static final AmazonDynamoDB client = DynamoDBEmbedded.create().amazonDynamoDB();
    protected static final UriRetriever uriRetriever = mock(UriRetriever.class);
    protected static final ChannelClaimClient channelClaimClient = mock(ChannelClaimClient.class);
    protected static final CustomerService customerService = mock(CustomerService.class);
    protected static final FakeCristinUnitsUtil cristinUnitsUtil = new FakeCristinUnitsUtil();

    public ResourcesLocalTest() {
        super();
    }

    public void init() {
        init(RESOURCES_TABLE_NAME);
    }

    public void init(String tableName) {
        try {
            client.deleteTable(tableName);
        } catch (ResourceNotFoundException ignored) {}
        var request = createTableRequest(tableName);
        client.createTable(request);
    }

    public void init(String firstTable, String secondTable) {
        init(firstTable);
        init(secondTable);
    }

    protected Resource persistResource(Resource resource) {
        client.transactWriteItems(new TransactWriteItemsRequest()
                                      .withTransactItems(new TransactWriteItem()
                                                             .withPut(new Put()
                                                                          .withItem(resource.toDao().toDynamoFormat())
                                                                          .withTableName(RESOURCES_TABLE_NAME))));
        resource.getPublicationChannels()
            .forEach(channel -> client.transactWriteItems(channel.toDao().createInsertionTransactionRequest()));
        resource.getFileEntries()
            .forEach(entry -> client.transactWriteItems(entry.toDao().createInsertionTransactionRequest()));
        return resource;
    }

    private CreateTableRequest createTableRequest(String tableName) {
        return new CreateTableRequest()
                   .withTableName(tableName)
                   .withAttributeDefinitions(attributeDefinitions())
                   .withKeySchema(primaryKeySchema())
                   .withGlobalSecondaryIndexes(globalSecondaryIndexes())
                   .withBillingMode(BillingMode.PAY_PER_REQUEST);
    }

    private Collection<GlobalSecondaryIndex> globalSecondaryIndexes() {
        List<GlobalSecondaryIndex> indexes = new ArrayList<>();
        indexes.add(
            newGsi(GSI_1_INDEX_NAME,
                   GSI_1_PARTITION_KEY_NAME,
                   GSI_1_SORT_KEY_NAME)
        );
        indexes.add(
            newGsi(BY_CUSTOMER_RESOURCE_INDEX_NAME,
                   BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME,
                   BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME)
        );
        indexes.add(
            newGsi(BY_TYPE_AND_IDENTIFIER_INDEX_NAME,
                   BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME,
                   BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
        );
        indexes.add(
            newGsi(RESOURCE_BY_CRISTIN_ID_INDEX_NAME,
                   RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME,
                   RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME)
        );
        return indexes;
    }

    private GlobalSecondaryIndex newGsi(String indexName, String partitionKeyName, String sortKeyName) {
        return new GlobalSecondaryIndex()
                   .withIndexName(indexName)
                   .withKeySchema(keySchema(partitionKeyName, sortKeyName))
                   .withProjection(new Projection().withProjectionType(ProjectionType.ALL));
    }

    private Collection<KeySchemaElement> primaryKeySchema() {
        return keySchema(PRIMARY_KEY_PARTITION_KEY_NAME, PRIMARY_KEY_SORT_KEY_NAME);
    }

    private Collection<KeySchemaElement> keySchema(String hashKey, String rangeKey) {
        List<KeySchemaElement> primaryKey = new ArrayList<>();
        primaryKey.add(newKeyElement(hashKey, KeyType.HASH));
        primaryKey.add(newKeyElement(rangeKey, KeyType.RANGE));
        return primaryKey;
    }

    private KeySchemaElement newKeyElement(String primaryKeySortKeyName, KeyType range) {
        return new KeySchemaElement().withAttributeName(primaryKeySortKeyName).withKeyType(range);
    }

    private AttributeDefinition[] attributeDefinitions() {
        List<AttributeDefinition> attributesList = new ArrayList<>();
        attributesList.add(newAttribute(PRIMARY_KEY_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(PRIMARY_KEY_SORT_KEY_NAME));
        attributesList.add(newAttribute(GSI_1_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(GSI_1_SORT_KEY_NAME));
        attributesList.add(newAttribute(BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME));
        attributesList.add(newAttribute(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME));
        attributesList.add(newAttribute(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME));
        AttributeDefinition[] attributesArray = new AttributeDefinition[attributesList.size()];
        attributesList.toArray(attributesArray);
        return attributesArray;
    }

    private AttributeDefinition newAttribute(String keyName) {
        return new AttributeDefinition()
                   .withAttributeName(keyName)
                   .withAttributeType(STRING_TYPE);
    }

    public TicketService getTicketService() {
        return new TicketService(client, uriRetriever, cristinUnitsUtil);
    }

    public ResourceService getResourceService(AmazonDynamoDB dynamoDbClient) {
        return getResourceService(dynamoDbClient, RESOURCES_TABLE_NAME);
    }

    public ResourceService getResourceService(AmazonDynamoDB dynamoDbClient, String tableName) {
        return new ResourceService(dynamoDbClient, tableName, Clock.systemDefaultZone(), uriRetriever,
                                   channelClaimClient, customerService, new FakeCristinUnitsUtil());
    }

    public MessageService getMessageService() {
        return new MessageService(client, uriRetriever, cristinUnitsUtil);
    }
}
