package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import org.junit.rules.ExternalResource;

import java.util.Arrays;
import java.util.List;

import static com.amazonaws.services.dynamodbv2.model.BillingMode.PAY_PER_REQUEST;
import static com.amazonaws.services.dynamodbv2.model.ScalarAttributeType.S;

public class PublicationsDynamoDBLocal extends ExternalResource {

    public static final String IDENTIFIER = "identifier";
    public static final String MODIFIED_DATE = "modifiedDate";
    public static final String PUBLISHER_ID = "publisherId";
    public static final String PUBLISHER_OWNER_DATE = "publisherOwnerDate";
    public static final String CREATED_DATE = "createdDate";
    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String STATUS = "status";
    public static final String OWNER = "owner";

    public static final String NVA_RESOURCES_TABLE_NAME = "nva_resources";
    public static final String BY_PUBLISHER_INDEX_NAME = "ByPublisher";

    private AmazonDynamoDB ddb;
    private DynamoDB client;

    @Override
    protected void before() throws Throwable {
        super.before();
        ddb = DynamoDBEmbedded.create().amazonDynamoDB();
        createPublicationsTable(ddb);
        client = new DynamoDB(ddb);
    }

    public Table getTable() {
        return client.getTable(NVA_RESOURCES_TABLE_NAME);
    }

    public Index getByPublisherIndex() {
        return getTable().getIndex(BY_PUBLISHER_INDEX_NAME);
    }

    private CreateTableResult createPublicationsTable(AmazonDynamoDB ddb) {
        List<AttributeDefinition> attributeDefinitions = Arrays.asList(
                new AttributeDefinition(IDENTIFIER, S),
                new AttributeDefinition(MODIFIED_DATE, S),
                new AttributeDefinition(PUBLISHER_ID, S),
                new AttributeDefinition(PUBLISHER_OWNER_DATE, S)
        );

        List<KeySchemaElement> keySchema = Arrays.asList(
                new KeySchemaElement(IDENTIFIER, KeyType.HASH),
                new KeySchemaElement(MODIFIED_DATE, KeyType.RANGE)
        );

        List<KeySchemaElement> byPublisherKeySchema = Arrays.asList(
                new KeySchemaElement(PUBLISHER_ID, KeyType.HASH),
                new KeySchemaElement(PUBLISHER_OWNER_DATE, KeyType.RANGE)
        );

        Projection byPublisherProjection = new Projection()
                .withProjectionType(ProjectionType.INCLUDE)
                .withNonKeyAttributes(IDENTIFIER, CREATED_DATE, MODIFIED_DATE, ENTITY_DESCRIPTION, STATUS, OWNER);

        List<GlobalSecondaryIndex> globalSecondaryIndexes = Arrays.asList(
                new GlobalSecondaryIndex()
                .withIndexName(BY_PUBLISHER_INDEX_NAME)
                .withKeySchema(byPublisherKeySchema)
                .withProjection(byPublisherProjection)
        );

        CreateTableRequest createTableRequest =
                new CreateTableRequest()
                .withTableName(NVA_RESOURCES_TABLE_NAME)
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(keySchema)
                .withGlobalSecondaryIndexes(globalSecondaryIndexes)
                .withBillingMode(PAY_PER_REQUEST);

        return ddb.createTable(createTableRequest);
    }

    @Override
    protected void after() {
        super.after();
        if (ddb != null) {
            ddb.shutdown();
        }
    }
}
