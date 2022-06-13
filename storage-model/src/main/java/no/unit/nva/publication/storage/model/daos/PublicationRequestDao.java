package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.PublicationRequest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;


@JsonTypeName(PublicationRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PublicationRequestDao extends Dao<PublicationRequest>
        implements
        JoinWithResource,
        JsonSerializable {

    public static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "d";
    public static final String TYPE = "PublicationRequest";
    private PublicationRequest data;


    @JacocoGenerated
    public PublicationRequestDao() {
        super();
    }

    public PublicationRequestDao(PublicationRequest data) {
        super();
        this.data = data;
    }

    public static PublicationRequestDao queryObject(URI publisherId, String owner, SortableIdentifier requestIdentifier) {
        PublicationRequest apr = PublicationRequest.builder()
                .withIdentifier(requestIdentifier)
                .withOwner(owner)
                .withCustomerId(publisherId)
                .build();

        return new PublicationRequestDao(apr);
    }

    public static PublicationRequestDao queryObject(URI publisherId, String owner) {
        return queryObject(publisherId, owner, null);
    }

    public static PublicationRequestDao queryByCustomerAndResourceIdentifier(UserInstance resourceOwner,
                                                                             SortableIdentifier resourceIdentifier) {
        PublicationRequest apr = PublicationRequest.builder()
                .withResourceIdentifier(resourceIdentifier)
                .withOwner(resourceOwner.getUserIdentifier())
                .withCustomerId(resourceOwner.getOrganizationUri())
                .build();
        return new PublicationRequestDao(apr);
    }

    @Override
    public PublicationRequest getData() {
        return data;
    }

    @Override
    public void setData(PublicationRequest data) {
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
        return PublicationRequest.TYPE;
    }


    @JsonIgnore
    public static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + DatabaseConstants.KEY_FIELDS_DELIMITER + PublicationRequest.TYPE;
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
        if (!(o instanceof PublicationRequestDao)) {
            return false;
        }
        PublicationRequestDao that = (PublicationRequestDao) o;
        return Objects.equals(getData(), that.getData());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }

}

