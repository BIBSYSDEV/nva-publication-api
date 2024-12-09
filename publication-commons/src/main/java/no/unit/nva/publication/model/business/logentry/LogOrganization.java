package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.clients.GetCustomerResponse;

@JsonTypeName(LogOrganization.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogOrganization(URI id, URI topLevelOrgCristinId, String shortName, String displayName) {

    public static final String TYPE = "LogOrganization";

    public static LogOrganization fromGetCustomerResponse(GetCustomerResponse getCustomerResponse) {
        return new LogOrganization(getCustomerResponse.id(), getCustomerResponse.cristinId(),
                                   getCustomerResponse.shortName(), getCustomerResponse.displayName());
    }

    public static LogOrganization fromCristinId(URI topLevelOrgCristinId) {
        return new LogOrganization(null, topLevelOrgCristinId, null, null);
    }
}
