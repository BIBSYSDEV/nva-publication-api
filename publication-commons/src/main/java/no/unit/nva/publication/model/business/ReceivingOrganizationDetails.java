package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(ReceivingOrganizationDetails.RECEIVING_ORGANIZATION_DETAILS)
public record ReceivingOrganizationDetails(URI topLevelOrganizationId, URI subOrganizationId,
                                           SortableIdentifier influencingChannelClaim) {

    static final String RECEIVING_ORGANIZATION_DETAILS = "ReceivingOrganizationDetails";

    public ReceivingOrganizationDetails(URI topLevelOrganizationId, URI subOrganizationId) {
        this(topLevelOrganizationId, subOrganizationId, null);
    }
}
