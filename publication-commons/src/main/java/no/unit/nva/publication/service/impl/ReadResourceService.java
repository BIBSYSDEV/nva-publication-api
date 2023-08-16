package no.unit.nva.publication.service.impl;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static java.util.Objects.isNull;
import static no.unit.nva.publication.PublicationServiceConfig.RESULT_SET_SIZE_FOR_DYNAMODB_QUERIES;
import static no.unit.nva.publication.model.business.Resource.resourceQueryObject;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ResourceService.EMPTY_RESOURCE_IDENTIFIER_ERROR;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.conditionValueMapToAttributeValueMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Contribution;
import no.unit.nva.publication.model.business.QuerySpliterator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.ContributionDao;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.ResourceDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class ReadResourceService {

    public static final String PUBLICATION_NOT_FOUND_CLIENT_MESSAGE = "Publication not found: ";

    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Could not find resource";
    public static final int DEFAULT_LIMIT = 100;
    private static final String ADDITIONAL_IDENTIFIER_CRISTIN = "Cristin";
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
        return getResource(userInstance, resourceIdentifier).toPublication();
    }

    public Publication getPublication(Publication publication) throws NotFoundException {
        return getResource(Resource.fromPublication(publication)).toPublication();
    }

    public List<Publication> getResourcesByOwner(UserInstance userInstance) {
        var partitionKey = constructPrimaryPartitionKey(userInstance);
        var querySpec = partitionKeyToQuerySpec(partitionKey);
        var valuesMap = conditionValueMapToAttributeValueMap(querySpec.getValueMap(), String.class);
        var namesMap = querySpec.getNameMap();
        var daos = sendAndParseQueryToDaos(querySpec.getKeyConditionExpression(), valuesMap, namesMap);

        return daosToPublications(daos)
                   .stream()
                   .limit(DEFAULT_LIMIT)
                   .collect(Collectors.toList());
    }

    protected Resource getResource(Resource resource) throws NotFoundException {
        var partitionKey = new ResourceDao(resource).getPrimaryKeyPartitionKey();
        var sortKey = new ResourceDao(resource).getPrimaryKeySortKey();
        var querySpec = primaryKeyToQuerySpec(partitionKey, sortKey);
        var valuesMap = conditionValueMapToAttributeValueMap(querySpec.getValueMap(), String.class);
        var namesMap = querySpec.getNameMap();
        var daos = sendAndParseQueryToDaos(querySpec.getKeyConditionExpression(), valuesMap, namesMap);

        return queryDaosToResource(daos);
    }


    public Resource getResourceByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        var queryObject = new ResourceDao(resourceQueryObject(identifier));
        var queryResult = queryObject.fetchByIdentifier(client, tableName);
        var resource = (Resource) queryResult.getData();
        var contributions = fetchAllContributionsForResource(resource).collect(Collectors.toList()); // TODO: Run paralell with last call
        return joinWithContributions(resource, contributions);
    }

    public List<Publication> getPublicationsByCristinIdentifier(String cristinIdentifier) {
        var queryObject = new ResourceDao(resourceQueryObjectWithCristinIdentifier(cristinIdentifier));
        var queryRequest = queryObject.createQueryFindByCristinIdentifier();
        var daos = sendAndParseRequestToDaos(queryRequest);
        return daosToPublications(daos);
    }

    protected Resource getResource(UserInstance userInstance, SortableIdentifier identifier) throws NotFoundException {
        return getResource(resourceQueryObject(userInstance, identifier));
    }


    protected List<Dao> fetchResourceAndAssociatedItemsFromResourceIndex(UserInstance userInstance,
                                                                         SortableIdentifier resourceIdentifier) {
        ResourceDao queryObject = ResourceDao.queryObject(userInstance, resourceIdentifier);
        QueryRequest queryRequest = attempt(() -> queryByResourceIndex(queryObject)).orElseThrow();
        QueryResult queryResult = client.query(queryRequest);
        return parseResultSetToDaos(queryResult);
    }

    protected Stream<Contribution> fetchAllContributionsForResource(Resource resource) {
        var dao = (ResourceDao) resource.toDao();
        return dao.fetchAllContributions(client, tableName)
                   .stream()
                   .map(ContributionDao::getData)
                   .map(Contribution.class::cast);
    }

    private static List<Resource> joinDaosToResourcesWithContributions(List<Dao> daos) {
        var resourceDaos = filterDaos(daos, ResourceDao.class);
        var contributionDaos = filterDaos(daos, ContributionDao.class);

        var resourcesWithContributions =
            contributionDaos.stream().collect(Collectors.groupingBy(ContributionDao::getResourceIdentifier));

        return resourceDaos
                   .stream()
                   .map(r -> joinDaosWithContributions(r, resourcesWithContributions))
                   .collect(Collectors.toList());

    }

    private static Resource joinWithContributions(Resource resource, List<Contribution> allContributions) {
        if (allContributions.isEmpty()) {
            return resource;
        }

        var contributors = allContributions
                               .stream()
                               .map(Contribution::getContributor)
                               .collect(Collectors.toList());

        if (resource.getEntityDescription() == null ) {
            resource.setEntityDescription(new EntityDescription());
        }
        resource.getEntityDescription().setContributors(contributors);

        return resource;
    }

    private static Resource joinDaosWithContributions(ResourceDao resourceDao,
                                                Map<SortableIdentifier, List<ContributionDao>> allContributions) {
        var resource = resourceDao.getResource();
        var contributions = allContributions
                                .getOrDefault(resource.getIdentifier(), List.of())
                                .stream()
                                .map(ContributionDao::getContribution)
                                .collect(Collectors.toList());

        return joinWithContributions(resource, contributions);
    }

    private static Resource queryDaosToResource(List<Dao> daos) throws NotFoundException {

        var resourceDaos = filterDaos(daos, ResourceDao.class);

        if (resourceDaos.isEmpty()) {
            throw new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE);
        }

        var resource = (Resource) resourceDaos.get(0).getData();

        if (resource.getEntityDescription() != null) {
            var contributionDaos = filterDaos(daos, ContributionDao.class);
            resource.getEntityDescription().setContributors(extractContributionDaos(contributionDaos));
        }
        return resource;
    }

    private static List<Dao> queryResultToDao(QueryResult result) {
        return result.getItems()
            .stream()
            .map(resultValuesMap -> parseAttributeValuesMap(resultValuesMap, Dao.class))
            .collect(Collectors.toList());
    }

    private static <T> List<T> filterDaos(List<Dao> daos, Class<T> tClass ) {
        return daos.stream().filter(tClass::isInstance)
                   .map(tClass::cast).collect(Collectors.toList());
    }

    private static List<Contributor> extractContributionDaos(List<ContributionDao> contributionDaos) {
        return contributionDaos.stream()
                   .map(ContributionDao::getContribution)
                   .map(Contribution::getContributor)
                   .collect(Collectors.toList());
    }

    private Resource resourceQueryObjectWithCristinIdentifier(String cristinIdentifier) {
        var resource = new Resource();
        resource.setAdditionalIdentifiers(
            Set.of(new AdditionalIdentifier(ADDITIONAL_IDENTIFIER_CRISTIN, cristinIdentifier)));
        return resource;
    }

    private String constructPrimaryPartitionKey(UserInstance userInstance) {
        return ResourceDao.constructPrimaryPartitionKey(userInstance.getOrganizationUri(),
                                                        userInstance.getUsername());
    }

    private List<Publication> daosToPublications(List<Dao> daos) {
        return joinDaosToResourcesWithContributions(daos)
                   .stream()
                   .map(Resource::toPublication)
                   .collect(Collectors.toList());
    }

    private List<Dao> sendAndParseQueryToDaos(String conditionExpression, Map<String, AttributeValue> valuesMap,
                                              Map<String, String> namesMap) {
        var request =
            new QueryRequest().withKeyConditionExpression(conditionExpression)
                .withExpressionAttributeNames(namesMap)
                .withExpressionAttributeValues(valuesMap)
                .withTableName(tableName);
        var queryIterator = new QuerySpliterator(client, request, RESULT_SET_SIZE_FOR_DYNAMODB_QUERIES);
        return StreamSupport.stream(queryIterator, false)
                   .map(item -> parseAttributeValuesMap(item, Dao.class)).collect(Collectors.toList());
    }

    private List<Dao> sendAndParseRequestToDaos(QueryRequest request) {

        var queryIterator = new QuerySpliterator(client, request, RESULT_SET_SIZE_FOR_DYNAMODB_QUERIES);
        return StreamSupport.stream(queryIterator, false)
                   .map(item -> parseAttributeValuesMap(item, Dao.class)).collect(Collectors.toList());
    }

    private QueryExpressionSpec partitionKeyToQuerySpec(String partitionKey) {
        return new ExpressionSpecBuilder()
                   .withKeyCondition(S(PRIMARY_KEY_PARTITION_KEY_NAME).eq(partitionKey)).buildForQuery();
    }

    private QueryExpressionSpec primaryKeyToQuerySpec(String partitionKey, String sortKey) {
        return new ExpressionSpecBuilder()
                   .withKeyCondition(
                       S(PRIMARY_KEY_PARTITION_KEY_NAME).eq(partitionKey)
                           .and(S(PRIMARY_KEY_SORT_KEY_NAME).beginsWith(sortKey))
                   )
                   .buildForQuery();
    }

    private QueryRequest queryByResourceIndex(ResourceDao queryObject) {
        var contributionsQueryObject = ContributionDao.queryObject(queryObject);
        Map<String, Condition> keyConditions = queryObject
                                                   .byResource(
                                                       queryObject.joinByResourceContainedOrderedType(),
                                                       contributionsQueryObject.joinByResourceContainedOrderedType()
                                                   );
        return new QueryRequest()
                   .withTableName(tableName)
                   .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                   .withKeyConditions(keyConditions);
    }

    private List<Dao> parseResultSetToDaos(QueryResult queryResult) {
        return queryResult.getItems()
                   .stream()
                   .map(values -> parseAttributeValuesMap(values, Dao.class))
                   .collect(Collectors.toList());
    }
}
