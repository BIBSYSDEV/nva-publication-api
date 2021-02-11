package no.unit.nva.publication.service.impl;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static java.util.Objects.isNull;
import static no.unit.nva.publication.service.impl.ResourceService.EMPTY_RESOURCE_IDENTIFIER_ERROR;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.conditionValueMapToAttributeValueMap;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ServiceWithTransactions.RAWTYPES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.Resource.resourceQueryObject;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.exceptions.BadRequestException;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.DoiRequestDao;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadResourceService {

    public static final String RESOURCE_BY_IDENTIFIER_NOT_FOUND_ERROR_PREFIX = "Could not find resource with "
                                                                               + "identifier: ";
    public static final String RESOURCE_BY_IDENTIFIER_NOT_FOUND_ERROR = RESOURCE_BY_IDENTIFIER_NOT_FOUND_ERROR_PREFIX
                                                                        + "{}, {} ";
    public static final String PUBLICATION_NOT_FOUND_CLIENT_MESSAGE = "Publication not found: ";

    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Could not find resource";

    private static final Logger logger = LoggerFactory.getLogger(ReadResourceService.class);

    private final AmazonDynamoDB client;
    private final String tableName;

    protected ReadResourceService(AmazonDynamoDB client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    public Publication getPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        if (isNull(resourceIdentifier)) {
            throw new BadRequestException(EMPTY_RESOURCE_IDENTIFIER_ERROR);
        }
        return getResource(userInstance, resourceIdentifier)
            .toPublication();
    }

    public Publication getPublication(Publication publication) throws NotFoundException {
        return getResource(Resource.fromPublication(publication)).toPublication();
    }

    public List<Publication> getResourcesByOwner(UserInstance userInstance) {
        String partitionKey = constructPrimaryPartitionKey(userInstance);
        QueryExpressionSpec querySpec = partitionKeyToQuerySpec(partitionKey);
        Map<String, AttributeValue> valuesMap = conditionValueMapToAttributeValueMap(querySpec.getValueMap(),
            String.class);
        Map<String, String> namesMap = querySpec.getNameMap();
        QueryResult result = performQuery(querySpec.getKeyConditionExpression(), valuesMap, namesMap);

        return queryResultToListOfPublications(result);
    }

    private String constructPrimaryPartitionKey(UserInstance userInstance) {
        return ResourceDao.constructPrimaryPartitionKey(userInstance.getOrganizationUri(),
            userInstance.getUserIdentifier());
    }

    private List<Publication> queryResultToListOfPublications(QueryResult result) {
        return queryResultToResourceList(result)
            .stream()
            .map(Resource::toPublication)
            .collect(Collectors.toList());
    }

    public Publication getPublicationByIdentifier(SortableIdentifier identifier) throws NotFoundException {

        QueryRequest queryRequest = createGetByResourceIdentifierQueryRequest(identifier);

        QueryResult result = client.query(queryRequest);
        ResourceDao fetchedDao = queryResultToSingleResource(identifier, result);

        return fetchedDao.getData().toPublication();
    }

    protected Resource getResource(UserInstance userInstance, SortableIdentifier identifier) throws NotFoundException {
        return getResource(resourceQueryObject(userInstance, identifier));
    }

    protected Resource getResource(Resource resource) throws NotFoundException {
        Map<String, AttributeValue> primaryKey = new ResourceDao(resource).primaryKey();
        GetItemResult getResult = getResourceByPrimaryKey(primaryKey);
        ResourceDao fetchedDao = parseAttributeValuesMap(getResult.getItem(), ResourceDao.class);
        return fetchedDao.getData();
    }

    @SuppressWarnings(RAWTYPES)
    protected List<Dao> fetchResourceAndDoiRequestFromTheByResourceIndex(UserInstance userInstance,
                                                                         SortableIdentifier resourceIdentifier) {
        ResourceDao queryObject = ResourceDao.queryObject(userInstance, resourceIdentifier);
        QueryRequest queryRequest = queryByResourceIndex(queryObject);
        QueryResult queryResult = client.query(queryRequest);
        return parseResultSetToDaos(queryResult);
    }

    private static ResourceDao queryResultToSingleResource(SortableIdentifier identifier, QueryResult result)
        throws NotFoundException {
        return result.getItems()
            .stream()
            .map(item -> parseAttributeValuesMap(item, ResourceDao.class))
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(fail -> handleGetResourceByIdentifierError(fail, identifier));
    }

    private static NotFoundException handleGetResourceByIdentifierError(Failure<ResourceDao> fail,
                                                                        SortableIdentifier identifier) {
        logger.warn(RESOURCE_BY_IDENTIFIER_NOT_FOUND_ERROR, identifier.toString(), fail.getException());
        return new NotFoundException(PUBLICATION_NOT_FOUND_CLIENT_MESSAGE + identifier.toString());
    }

    private static List<Resource> queryResultToResourceList(QueryResult result) {
        return result.getItems()
            .stream()
            .map(resultValuesMap -> parseAttributeValuesMap(resultValuesMap, ResourceDao.class))
            .map(ResourceDao::getData)
            .collect(Collectors.toList());
    }

    private QueryRequest createGetByResourceIdentifierQueryRequest(SortableIdentifier identifier) {

        Resource resource = resourceQueryObject(identifier);
        ResourceDao resourceDao = new ResourceDao(resource);

        String keyCondition = "#PK = :PK AND #SK = :SK";
        Map<String, String> expressionAttributeName =
            Map.of(
                "#PK", DatabaseConstants.RESOURCES_BY_IDENTIFIER_INDEX_PARTITION_KEY_NAME,
                "#SK", DatabaseConstants.RESOURCES_BY_IDENTIFIER_INDEX_SORT_KEY_NAME
            );
        Map<String, AttributeValue> expressionAttributeValues =
            Map.of(":PK", new AttributeValue(resourceDao.getResourceByIdentifierPartitionKey()),
                ":SK", new AttributeValue(resourceDao.getResourceByIdentifierSortKey())
            );

        return new QueryRequest()
            .withTableName(tableName)
            .withIndexName(DatabaseConstants.RESOURCES_BY_IDENTIFIER_INDEX_NAME)
            .withKeyConditionExpression(keyCondition)
            .withExpressionAttributeNames(expressionAttributeName)
            .withExpressionAttributeValues(expressionAttributeValues);
    }

    private QueryResult performQuery(String conditionExpression, Map<String, AttributeValue> valuesMap,
                                     Map<String, String> namesMap) {
        return client.query(
            new QueryRequest().withKeyConditionExpression(conditionExpression)
                .withExpressionAttributeNames(namesMap)
                .withExpressionAttributeValues(valuesMap)
                .withTableName(tableName)
        );
    }

    private QueryExpressionSpec partitionKeyToQuerySpec(String partitionKey) {
        return new ExpressionSpecBuilder()
            .withKeyCondition(S(PRIMARY_KEY_PARTITION_KEY_NAME).eq(partitionKey)).buildForQuery();
    }

    private GetItemResult getResourceByPrimaryKey(Map<String, AttributeValue> primaryKey) throws NotFoundException {
        GetItemResult result = client.getItem(new GetItemRequest()
            .withTableName(tableName)
            .withKey(primaryKey));
        if (isNull(result.getItem())) {
            throw new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE);
        }
        return result;
    }

    private QueryRequest queryByResourceIndex(ResourceDao queryObject) {
        Map<String, Condition> keyConditions = queryObject
            .byResource(
                DoiRequestDao.joinByResourceContainedOrderedType(),
                ResourceDao.joinByResourceContainedOrderedType()
            );
        return new QueryRequest()
            .withTableName(tableName)
            .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
            .withKeyConditions(keyConditions);
    }

    @SuppressWarnings(RAWTYPES)
    private List<Dao> parseResultSetToDaos(QueryResult queryResult) {
        return queryResult.getItems()
            .stream()
            .map(values -> parseAttributeValuesMap(values, Dao.class))
            .collect(Collectors.toList());
    }
}
