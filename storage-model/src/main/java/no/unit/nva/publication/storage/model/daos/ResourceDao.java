package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceByIdentifier;
import no.unit.nva.publication.storage.model.UserInstance;

@JsonTypeName("Resource")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ResourceDao extends Dao<Resource>
    implements JoinWithResource,
               ResourceByIdentifier {

    private static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "b";
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
    public static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + DatabaseConstants.KEY_FIELDS_DELIMITER + Resource.getType();
    }

    @JsonIgnore
    public static String getContainedType() {
        return Resource.getType();
    }

    @Override
    public Resource getData() {
        return data;
    }

    @Override
    public void setData(Resource resource) {
        this.data = resource;
    }

    @Override
    public String getType() {
        return Resource.getType();
    }

    @Override
    public URI getCustomerId() {
        return data.getPublisher().getId();
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return data.getIdentifier();
    }

    @Override
    protected String getOwner() {
        return data.getOwner();
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
    public int hashCode() {
        return Objects.hash(getData());
    }

    @Override
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
}
