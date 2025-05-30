package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(ReceivingOrganizationDetails.RECEIVING_ORGANIZATION_DETAILS)
public record ReceivingOrganizationDetails(URI topLevelOrganizationId, URI subOrganizationId) {

    static final String RECEIVING_ORGANIZATION_DETAILS = "ReceivingOrganizationDetails";

    public URI resolveOrganizationBySpecificity() {
        return Optional.ofNullable(subOrganizationId).orElse(topLevelOrganizationId);
    }
}
