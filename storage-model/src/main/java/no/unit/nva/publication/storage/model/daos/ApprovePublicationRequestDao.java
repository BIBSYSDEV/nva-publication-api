package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.ApprovePublicationRequest;
import no.unit.nva.publication.storage.model.DatabaseConstants;

import java.net.URI;


@JsonTypeName(ApprovePublicationRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ApprovePublicationRequestDao extends Dao<ApprovePublicationRequest>
        implements
        JoinWithResource,
        JsonSerializable {

    public static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "d";
    public static final String TYPE = "ApprovePublicationRequest";
    private ApprovePublicationRequest data;

    public ApprovePublicationRequestDao(ApprovePublicationRequest data) {
        this.data = data;
    }

    public static ApprovePublicationRequestDao queryObject(URI sampleCustomer, String sampleUser) {
        ApprovePublicationRequest approvePublicationRequest = new ApprovePublicationRequest();
        return new ApprovePublicationRequestDao(approvePublicationRequest);
    }

    public static ApprovePublicationRequestDao queryObject(URI sampleCustomer, String sampleUser, SortableIdentifier sampleEntryIdentifier) {
        ApprovePublicationRequest approvePublicationRequest = new ApprovePublicationRequest();
        return new ApprovePublicationRequestDao(approvePublicationRequest);
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
        return TYPE;
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

    @JsonIgnore
    public static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + DatabaseConstants.KEY_FIELDS_DELIMITER + ApprovePublicationRequest.TYPE;
    }

}

