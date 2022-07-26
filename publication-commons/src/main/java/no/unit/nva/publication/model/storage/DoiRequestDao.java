package no.unit.nva.publication.model.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(DoiRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class DoiRequestDao extends Dao<DoiRequest>
    implements
    JoinWithResource,
    JsonSerializable {
    
    public static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "a";
    public static final String TYPE = "DoiRequest";
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
    
    public static DoiRequestDao queryByCustomerAndResourceIdentifier(UserInstance resourceOwner,
                                                                     SortableIdentifier resourceIdentifier) {
        DoiRequest doi = DoiRequest.builder()
            .withResourceIdentifier(resourceIdentifier)
            .withOwner(resourceOwner.getUserIdentifier())
            .withCustomerId(resourceOwner.getOrganizationUri())
            .build();
        return new DoiRequestDao(doi);
    }
    
    public static DoiRequestDao queryObject(ResourceDao queryObject) {
        var doiRequest = DoiRequest.builder()
            .withResourceIdentifier(queryObject.getResourceIdentifier())
            .build();
        return new DoiRequestDao(doiRequest);
    }
    
    public static String getContainedType() {
        return DoiRequest.TYPE;
    }
    
    public String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + DatabaseConstants.KEY_FIELDS_DELIMITER + data.getType();
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
