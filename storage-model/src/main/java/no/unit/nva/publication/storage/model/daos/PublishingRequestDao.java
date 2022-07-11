package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_FORMAT;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;


@JsonTypeName(PublishingRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PublishingRequestDao extends Dao<PublishingRequestCase>
        implements JoinWithResource, JsonSerializable {

    public static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "d";
    public static final String TYPE = "PublishingRequestCase";
    private PublishingRequestCase data;


    @JacocoGenerated
    public PublishingRequestDao() {
        super();
    }

    public PublishingRequestDao(PublishingRequestCase data) {
        super();
        this.data = data;
    }

    public static PublishingRequestDao queryObject(PublishingRequestCase queryObject) {
        return new PublishingRequestDao(queryObject);
    }

    public static PublishingRequestDao queryByCustomerAndResourceIdentifier(UserInstance resourceOwner,
                                                                            SortableIdentifier resourceIdentifier) {
        var queryObject =
            PublishingRequestCase.createQuery(resourceOwner, resourceIdentifier, null);
        return new PublishingRequestDao(queryObject);
    }

    @Override
    public PublishingRequestCase getData() {
        return data;
    }

    @Override
    public void setData(PublishingRequestCase data) {
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
    public String joinByResourceOrderedType() {
        return joinByResourceContainedOrderedType();
    }

    @Override
    public SortableIdentifier getResourceIdentifier() {
        return data.getResourceIdentifier();
    }

    @Override
    protected String getOwner() {
        return data.getOwner();
    }

    public static String getContainedType() {
        return PublishingRequestCase.TYPE;
    }

    @Override
    public final String getPrimaryKeySortKey() {
        return String.format(PRIMARY_KEY_SORT_KEY_FORMAT, getType(), getIdentifier());
    }


    @JsonIgnore
    public static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + DatabaseConstants.KEY_FIELDS_DELIMITER + PublishingRequestCase.TYPE;
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
        if (!(o instanceof PublishingRequestDao)) {
            return false;
        }
        PublishingRequestDao that = (PublishingRequestDao) o;
        return Objects.equals(getData(), that.getData());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }

}

