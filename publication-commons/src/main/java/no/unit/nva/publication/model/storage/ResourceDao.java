package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

@JsonTypeName(ResourceDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ResourceDao extends Dao
    implements JoinWithResource, WithCristinIdentifier, JsonSerializable {
    
    public static final String CRISTIN_SOURCE = "Cristin";
    public static final String TYPE = "Resource";
    private static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "a";
    private Resource data;
    
    public ResourceDao() {
        this(new Resource());
    }
    
    public ResourceDao(Resource resource) {
        super();
        this.data = resource;
    }
    
    public static ResourceDao queryObject(UserInstance userInstance, SortableIdentifier resourceIdentifier) {
        Resource resource = Resource.emptyResource(
            userInstance.getUserIdentifier(),
            userInstance.getOrganizationUri(),
            resourceIdentifier);
        return new ResourceDao(resource);
    }
    
    public static String constructPrimaryPartitionKey(URI customerId, String owner) {
        return String.format(PRIMARY_KEY_PARTITION_KEY_FORMAT, Resource.TYPE,
            orgUriToOrgIdentifier(customerId), owner);
    }
    
    @JsonIgnore
    public String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + data.getType();
    }
    
    @JsonIgnore
    public String getContainedType() {
        return this.getContainedDataType();
    }
    
    @Override
    public Resource getData() {
        return data;
    }
    
    @Override
    public void setData(Entity resource) {
        this.data = (Resource) resource;
    }
    
    @Override
    public String getType() {
        return this.getData().getType();
    }
    
    @Override
    public URI getCustomerId() {
        return data.getPublisher().getId();
    }
    
    //TODO: cover when refactoring to ticket system is completed
    @JacocoGenerated
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected String getOwner() {
        return data.getOwner();
    }
    
    @Override
    public Optional<String> getCristinIdentifier() {
        String cristinIdentifierValue = Optional.ofNullable(data.getAdditionalIdentifiers())
            .stream()
            .flatMap(Collection::stream)
            .filter(this::keyEqualsCristin)
            .map(AdditionalIdentifier::getValue)
            .collect(SingletonCollector.collectOrElse(null));
        return Optional.ofNullable(cristinIdentifierValue);
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
    
    private boolean keyEqualsCristin(AdditionalIdentifier identifier) {
        return Optional.ofNullable(identifier)
            .map(AdditionalIdentifier::getSource)
            .map(CRISTIN_SOURCE::equals)
            .orElse(false);
    }
}
