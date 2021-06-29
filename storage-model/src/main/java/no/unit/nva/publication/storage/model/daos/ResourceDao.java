package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceByIdentifier;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.SingletonCollector;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;

@JsonTypeName("Resource")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ResourceDao extends Dao<Resource>
        implements JoinWithResource,
        ResourceByIdentifier,
        WithCristinIdentifier {

    private static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "b";
    public static final String CRISTIN_SOURCE = "Cristin";
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
        return BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + Resource.getType();
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
    public Optional<String> getCristinIdentifier() {
        String cristinIdentifierValue = Optional.ofNullable(data.getAdditionalIdentifiers())
                .stream()
                .flatMap(Collection::stream)
                .filter(this::keyEqualsCristin)
                .map(AdditionalIdentifier::getValue)
                .collect(SingletonCollector.collectOrElse(null));
        return Optional.ofNullable(cristinIdentifierValue);

    }

    private boolean keyEqualsCristin(AdditionalIdentifier identifier) {
        return identifier.getSource().equals(CRISTIN_SOURCE);
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
