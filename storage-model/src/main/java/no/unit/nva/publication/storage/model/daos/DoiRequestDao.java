package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

@JsonTypeName("DoiRequest")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class DoiRequestDao extends Dao<DoiRequest>
    implements
    JoinWithResource,
    JsonSerializable {

    public static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "a";

    private DoiRequest data;

    @JacocoGenerated
    public DoiRequestDao() {
        super();
    }

    public DoiRequestDao(DoiRequest doiRequest) {
        super();
        this.data = doiRequest;
    }

    public static DoiRequestDao queryObject(URI publisherId, String owner, SortableIdentifier doiRequestIdentifier) {
        DoiRequest doi = DoiRequest.builder()
            .withIdentifier(doiRequestIdentifier)
            .withOwner(owner)
            .withCustomerId(publisherId)
                             .build();
    
        return new DoiRequestDao(doi);
    }
    
    public static DoiRequestDao queryObject(URI publisherId, String owner) {
        return queryObject(publisherId, owner, null);
    }
    
    public static DoiRequestDao queryByCustomerAndResourceIdentifier(UserInstance userInstance,
                                                                     SortableIdentifier resourceIdentifier) {
        DoiRequest doi = DoiRequest.builder()
                             .withResourceIdentifier(resourceIdentifier)
                             .withOwner(userInstance.getUserIdentifier())
                             .withCustomerId(userInstance.getOrganizationUri())
                             .build();
        return new DoiRequestDao(doi);
    }
    
    @JsonIgnore
    public static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + DatabaseConstants.KEY_FIELDS_DELIMITER + DoiRequest.getType();
    }

    public static String getContainedType() {
        return DoiRequest.TYPE;
    }

    @Override
    public DoiRequest getData() {
        return data;
    }

    @Override
    public void setData(DoiRequest data) {
        this.data = data;
    }

    @Override
    public String getType() {
        return getContainedType();
    }

    @Override
    public URI getCustomerId() {
        return data.getCustomerId();
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
    @JsonIgnore
    public String joinByResourceOrderedType() {
        return joinByResourceContainedOrderedType();
    }

    @Override
    @JsonIgnore
    public SortableIdentifier getResourceIdentifier() {
        return data.getResourceIdentifier();
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
        if (!(o instanceof DoiRequestDao)) {
            return false;
        }
        DoiRequestDao that = (DoiRequestDao) o;
        return Objects.equals(getData(), that.getData());
    }
    
    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
