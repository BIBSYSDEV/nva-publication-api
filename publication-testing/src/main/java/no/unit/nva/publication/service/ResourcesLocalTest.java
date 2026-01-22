package no.unit.nva.publication.service;

import static java.util.Objects.nonNull;
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
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import java.net.URI;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.CouplingBetweenObjects"})
@JacocoGenerated
public class ResourcesLocalTest extends TestDataSource {

    public static final ScalarAttributeType STRING_TYPE = ScalarAttributeType.S;
    protected DynamoDbClient client;
    protected UriRetriever uriRetriever;
    protected ChannelClaimClient channelClaimClient;
    protected CustomerService customerService;
    protected FakeCristinUnitsUtil cristinUnitsUtil;

    private com.amazonaws.services.dynamodbv2.AmazonDynamoDB embeddedDynamoDb;

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
        cristinUnitsUtil = new FakeCristinUnitsUtil();

        embeddedDynamoDb = DynamoDBEmbedded.create().amazonDynamoDB();
        client = createSdk2Client();
        var request = createTableRequest(tableName);
        client.createTable(request);
    }

    public void init(String firstTable, String secondTable) {
        uriRetriever = mock(UriRetriever.class);
        customerService = mock(CustomerService.class);
        channelClaimClient = mock(ChannelClaimClient.class);
        cristinUnitsUtil = new FakeCristinUnitsUtil();

        embeddedDynamoDb = DynamoDBEmbedded.create().amazonDynamoDB();
        client = createSdk2Client();
        var firstTableRequest = createTableRequest(firstTable);
        var secondTableRequest = createTableRequest(secondTable);
        client.createTable(firstTableRequest);
        client.createTable(secondTableRequest);
    }

    private DynamoDbClient createSdk2Client() {
        var endpoint = URI.create("http://localhost:8000");
        return DynamoDbClient.builder()
                   .endpointOverride(endpoint)
                   .region(Region.EU_WEST_1)
                   .credentialsProvider(StaticCredentialsProvider.create(
                       AwsBasicCredentials.create("dummy", "dummy")))
                   .build();
    }

    protected Resource persistResource(Resource resource) {
        var putItem = Put.builder()
                          .item(resource.toDao().toDynamoFormat())
                          .tableName(RESOURCES_TABLE_NAME)
                          .build();
        var transactItem = TransactWriteItem.builder().put(putItem).build();
        client.transactWriteItems(TransactWriteItemsRequest.builder()
                                      .transactItems(transactItem)
                                      .build());
        resource.getPublicationChannels()
            .forEach(channel -> client.transactWriteItems(channel.toDao().createInsertionTransactionRequest()));
        resource.getFileEntries()
            .forEach(entry -> client.transactWriteItems(entry.toDao().createInsertionTransactionRequest()));
        return resource;
    }

    @AfterEach
    public void shutdown() {
        if (nonNull(embeddedDynamoDb)) {
            embeddedDynamoDb.shutdown();
        }
    }

    private CreateTableRequest createTableRequest(String tableName) {
        return CreateTableRequest.builder()
                   .tableName(tableName)
                   .attributeDefinitions(attributeDefinitions())
                   .keySchema(primaryKeySchema())
                   .globalSecondaryIndexes(globalSecondaryIndexes())
                   .billingMode(BillingMode.PAY_PER_REQUEST)
                   .build();
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
        return GlobalSecondaryIndex.builder()
                   .indexName(indexName)
                   .keySchema(keySchema(partitionKeyName, sortKeyName))
                   .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                   .build();
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
        return KeySchemaElement.builder()
                   .attributeName(primaryKeySortKeyName)
                   .keyType(range)
                   .build();
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
        return attributesList.toArray(new AttributeDefinition[0]);
    }

    private AttributeDefinition newAttribute(String keyName) {
        return AttributeDefinition.builder()
                   .attributeName(keyName)
                   .attributeType(STRING_TYPE)
                   .build();
    }

    public TicketService getTicketService() {
        return new TicketService(client, uriRetriever, cristinUnitsUtil);
    }

    public ResourceService getResourceService(DynamoDbClient dynamoDbClient) {
        return getResourceService(dynamoDbClient, RESOURCES_TABLE_NAME);
    }

    public ResourceService getResourceService(DynamoDbClient dynamoDbClient, String tableName) {
        return new ResourceService(dynamoDbClient, tableName, Clock.systemDefaultZone(), uriRetriever,
                                   channelClaimClient, customerService, new FakeCristinUnitsUtil());
    }

    public MessageService getMessageService() {
        return new MessageService(client, uriRetriever, cristinUnitsUtil);
    }
}
