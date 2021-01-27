package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.publication.storage.model.Resource;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ResourceDao extends Dao<Resource> {

    public static final String PATH_SEPARATOR = "/";

    private Resource data;

    public ResourceDao() {
        this(new Resource());
    }

    public ResourceDao(Resource resource) {
        super();
        this.data = resource;
    }

    public static String constructPrimaryPartitionKey(URI customerId, String owner) {
        return String.format(PRIMARY_KEY_PARTITION_KEY_FORMAT, Resource.TYPE,
            orgUriToOrgIdentifier(customerId), owner);
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
    protected String getType() {
        return Resource.getType();
    }

    @Override
    protected URI getCustomerId() {
        return data.getPublisher().getId();
    }

    @Override
    protected String getOwner() {
        return data.getOwner();
    }

    @Override
    protected String getIdentifier() {
        return data.getIdentifier().toString();
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
