package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;


@JsonTypeName(PublishingRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PublishingRequestDao extends Dao<PublishingRequest>
        implements
        JoinWithResource,
        JsonSerializable {

    public static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "d";
    public static final String TYPE = "PublishingRequest";
    private PublishingRequest data;


    @JacocoGenerated
    public PublishingRequestDao() {
        super();
    }

    public PublishingRequestDao(PublishingRequest data) {
        super();
        this.data = data;
    }

    public static PublishingRequestDao queryObject(URI publisherId, String owner, SortableIdentifier requestIdentifier) {
        PublishingRequest apr = PublishingRequest.builder()
                .withIdentifier(requestIdentifier)
                .withOwner(owner)
                .withCustomerId(publisherId)
                .build();

        return new PublishingRequestDao(apr);
    }

    public static PublishingRequestDao queryObject(URI publisherId, String owner) {
        return queryObject(publisherId, owner, null);
    }

    public static PublishingRequestDao queryByCustomerAndResourceIdentifier(UserInstance resourceOwner,
                                                                            SortableIdentifier resourceIdentifier) {
        PublishingRequest apr = PublishingRequest.builder()
                .withResourceIdentifier(resourceIdentifier)
                .withOwner(resourceOwner.getUserIdentifier())
                .withCustomerId(resourceOwner.getOrganizationUri())
                .build();
        return new PublishingRequestDao(apr);
    }

    @Override
    public PublishingRequest getData() {
        return data;
    }

    @Override
    public void setData(PublishingRequest data) {
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
        return PublishingRequest.TYPE;
    }


    @JsonIgnore
    public static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + DatabaseConstants.KEY_FIELDS_DELIMITER + PublishingRequest.TYPE;
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

