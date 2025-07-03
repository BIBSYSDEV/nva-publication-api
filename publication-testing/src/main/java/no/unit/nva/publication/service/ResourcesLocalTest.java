package no.unit.nva.publication.service;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
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
import org.junit.jupiter.api.AfterEach;

@SuppressWarnings({"PMD.TestClassWithoutTestCases"})
@JacocoGenerated
public class ResourcesLocalTest extends TestDataSource {

    public static final ScalarAttributeType STRING_TYPE = ScalarAttributeType.S;
    protected AmazonDynamoDB client;
    protected UriRetriever uriRetriever;
    protected ChannelClaimClient channelClaimClient;
    protected CustomerService customerService;

    public ResourcesLocalTest() {
        super();
    }

    public void init() {
        init(RESOURCES_TABLE_NAME);
    }

    public void init(String tableName) {
        uriRetriever = mock(UriRetriever.class);
        customerService = mock(CustomerService.class);
        channelClaimClient = mock(ChannelClaimClient.class);
        client = DynamoDBEmbedded.create().amazonDynamoDB();
        CreateTableRequest request = createTableRequest(tableName);
        client.createTable(request);
    }

    public void init(String firstTable, String secondTable) {
        uriRetriever = mock(UriRetriever.class);
        customerService = mock(CustomerService.class);
        channelClaimClient = mock(ChannelClaimClient.class);
        client = DynamoDBEmbedded.create().amazonDynamoDB();
        var firstTableRequest = createTableRequest(firstTable);
        var secondTableRequest = createTableRequest(secondTable);
        client.createTable(firstTableRequest);
        client.createTable(secondTableRequest);
    }

    protected Resource persistResource(Resource resource) {
        client.transactWriteItems(new TransactWriteItemsRequest()
                                      .withTransactItems(new TransactWriteItem()
                                                             .withPut(new Put()
                                                                          .withItem(resource.toDao().toDynamoFormat())
                                                                          .withTableName(RESOURCES_TABLE_NAME))));
        resource.getPublicationChannels()
            .forEach(channel -> client.transactWriteItems(channel.toDao().createInsertionTransactionRequest()));
        return resource;
    }

    @AfterEach
    public void shutdown() {
        if (nonNull(client)) {
            client.shutdown();
        }

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
            newGsi(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME,
                   BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME,
                   BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME
            )
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
        attributesList.add(newAttribute(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME));
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
        return new TicketService(client, uriRetriever);
    }

    public ResourceService getResourceService(AmazonDynamoDB dynamoDbClient) {
        return getResourceService(dynamoDbClient, RESOURCES_TABLE_NAME);
    }

    public ResourceService getResourceService(AmazonDynamoDB dynamoDbClient, String tableName) {
        return new ResourceService(dynamoDbClient, tableName, Clock.systemDefaultZone(), uriRetriever,
                                   channelClaimClient, customerService);
    }

    public MessageService getMessageService() {
        return new MessageService(client, uriRetriever);
    }
}
