package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.ApprovePublicationRequest;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;


@JsonTypeName(ApprovePublicationRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ApprovePublicationRequestDao extends Dao<ApprovePublicationRequest>
        implements
        JoinWithResource,
        JsonSerializable {

    public static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "d";
    public static final String TYPE = "ApprovePublicationRequest";
    private ApprovePublicationRequest data;


    @JacocoGenerated
    public ApprovePublicationRequestDao() {
        super();
    }

    public ApprovePublicationRequestDao(ApprovePublicationRequest data) {
        this.data = data;
    }

    public static ApprovePublicationRequestDao queryObject(URI publisherId, String owner, SortableIdentifier requestIdentifier) {
        ApprovePublicationRequest apr = ApprovePublicationRequest.builder()
                .withIdentifier(requestIdentifier)
                .withOwner(owner)
                .withCustomerId(publisherId)
                .build();

        return new ApprovePublicationRequestDao(apr);
    }

    public static ApprovePublicationRequestDao queryObject(URI publisherId, String owner) {
        return queryObject(publisherId, owner, null);
    }

    public static ApprovePublicationRequestDao queryByCustomerAndResourceIdentifier(UserInstance resourceOwner,
                                                                     SortableIdentifier resourceIdentifier) {
        ApprovePublicationRequest apr = ApprovePublicationRequest.builder()
                .withResourceIdentifier(resourceIdentifier)
                .withOwner(resourceOwner.getUserIdentifier())
                .withCustomerId(resourceOwner.getOrganizationUri())
                .build();
        return new ApprovePublicationRequestDao(apr);
    }

    @Override
    public ApprovePublicationRequest getData() {
        return data;
    }

    @Override
    public void setData(ApprovePublicationRequest data) {
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
        return ApprovePublicationRequest.TYPE;
    }


    @JsonIgnore
    public static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + DatabaseConstants.KEY_FIELDS_DELIMITER + ApprovePublicationRequest.TYPE;
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
        if (!(o instanceof ApprovePublicationRequestDao)) {
            return false;
        }
        ApprovePublicationRequestDao that = (ApprovePublicationRequestDao) o;
        return Objects.equals(getData(), that.getData());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }

}

