package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CRISTIN_IDENTIFIER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

@JsonTypeName(ResourceDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ResourceDao extends Dao
    implements JoinWithResource, JsonSerializable, DynamoEntryByIdentifier {
    
    public static final String CRISTIN_SOURCE = "Cristin";
    public static final String TYPE = "Resource";
    private static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "a";
    private static final String STATUS_FIELD = "status";
    private static final String MODIFIED_DATA_FIELD = "modifiedDate";
    private static final String DOI_FIELD = "doi";

    @JsonProperty(STATUS_FIELD)
    private PublicationStatus status;

    @JsonProperty(MODIFIED_DATA_FIELD)
    private Instant modifiedDate;
    @JsonProperty(DOI_FIELD)
    private URI doi;
    
    public ResourceDao() {
        this(new Resource());
    }
    
    public ResourceDao(Resource resource) {
        super(resource);
        setIdentifier(resource.getIdentifier());
        this.status = resource.getStatus();
        this.modifiedDate = resource.getModifiedDate();
        this.doi = resource.getDoi();
    }
    
    public static ResourceDao queryObject(UserInstance userInstance, SortableIdentifier resourceIdentifier) {
        Resource resource = Resource.emptyResource(
            userInstance.getUser(),
            userInstance.getCustomerId(),
            resourceIdentifier);
        return new ResourceDao(resource);
    }
    
    public static String constructPrimaryPartitionKey(URI customerId, String owner) {
        return String.format(PRIMARY_KEY_PARTITION_KEY_FORMAT, Resource.TYPE,
            orgUriToOrgIdentifier(customerId), owner);
    }
    
    @JsonIgnore
    public String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + getData().getType();
    }
    
    @Override
    public String indexingType() {
        return this.getData().getType();
    }
    
    @Override
    public URI getCustomerId() {
        return getResource().getPublisher().getId();
    }
    
    //TODO: cover when refactoring to ticket system is completed
    @JacocoGenerated
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        throw new UnsupportedOperationException();
    }
    
    @JacocoGenerated
    @Override
    public void updateExistingEntry(AmazonDynamoDB client) {
        throw new UnsupportedOperationException("Not implemented yet.Call the appropriate resource service method");
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getData());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceDao)) {
            return false;
        }
        ResourceDao that = (ResourceDao) o;
        return Objects.equals(getData(), that.getData());
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public URI getDoi() {
        return doi;
    }

    public void setDoi(URI doi) {
        this.doi = doi;
    }

    @Override
    protected User getOwner() {
        return getData().getOwner();
    }
    
    public List<TicketDao> fetchAllTickets(AmazonDynamoDB client) {
        var queryRequest = new QueryRequest()
                               .withTableName(RESOURCES_TABLE_NAME)
                               .withIndexName(DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME)
                               .withKeyConditions(joinAllRelatedTicketsForResource());
        return client.query(queryRequest)
                   .getItems()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, TicketDao.class))
                   .collect(Collectors.toList());
    }
    
    public ResourceDao fetchForElevatedUser(AmazonDynamoDB client) throws NotFoundException {
        var queryRequest = new QueryRequest()
                               .withTableName(RESOURCES_TABLE_NAME)
                               .withIndexName(DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME)
                               .withKeyConditions(byResource(ResourceDao.BY_RESOURCE_INDEX_ORDER_PREFIX));
        return client.query(queryRequest)
                   .getItems()
                   .stream()
                   .map(item -> parseAttributeValuesMap(item, ResourceDao.class))
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(fail -> new NotFoundException("Publication not found"));
    }
    
    @JsonProperty(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME)
    public String getResourceByCristinIdentifierPartitionKey() {
        return extractCristinIdentifier().isEmpty() ? null
                   : CRISTIN_IDENTIFIER_INDEX_FIELD_PREFIX + KEY_FIELDS_DELIMITER + extractCristinIdentifier();
    }
    
    @JsonProperty(RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME)
    public String getResourceByCristinIdentifierSortKey() {
        return indexingType() + KEY_FIELDS_DELIMITER + getIdentifier();
    }
    
    public QueryRequest createQueryFindByCristinIdentifier() {
        return new QueryRequest()
                   .withTableName(RESOURCES_TABLE_NAME)
                   .withIndexName(RESOURCE_BY_CRISTIN_ID_INDEX_NAME)
                   .withKeyConditions(createConditionsWithCristinIdentifier());
    }
    
    public Map<String, Condition> createConditionsWithCristinIdentifier() {
        Condition condition = new Condition()
                                  .withComparisonOperator(ComparisonOperator.EQ)
                                  .withAttributeValueList(
                                      new AttributeValue(getResourceByCristinIdentifierPartitionKey()));
        return Map.of(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME, condition);
    }
    
    public Optional<String> extractCristinIdentifier() {
        String cristinIdentifierValue = Optional.ofNullable(getResource().getAdditionalIdentifiers())
                                            .stream()
                                            .flatMap(Collection::stream)
                                            .filter(this::keyEqualsCristin)
                                            .map(AdditionalIdentifier::getValue)
                                            .collect(SingletonCollector.collectOrElse(null));
        return Optional.ofNullable(cristinIdentifierValue);
    }
    
    @JsonIgnore
    public Resource getResource() {
        return (Resource) getData();
    }
    
    @Override
    public String joinByResourceOrderedType() {
        return joinByResourceContainedOrderedType();
    }
    
    @Override
    @JsonIgnore
    public SortableIdentifier getResourceIdentifier() {
        return this.getIdentifier();
    }
    
    private boolean keyEqualsCristin(AdditionalIdentifier identifier) {
        return Optional.ofNullable(identifier)
                   .map(AdditionalIdentifier::getSourceName)
                   .map(CRISTIN_SOURCE::equals)
                   .orElse(false);
    }
}
