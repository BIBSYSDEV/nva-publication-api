package no.unit.nva.publication.model.business.logentry;

import java.net.URI;
import no.unit.nva.clients.GetCustomerResponse;

public record LogInstitution(URI id, URI topLevelOrgCristinId, String shortName, String displayName) {

    public static LogInstitution fromGetUserResponse(GetCustomerResponse getCustomerResponse) {
        return new LogInstitution(getCustomerResponse.id(), getCustomerResponse.cristinId(),
                                  getCustomerResponse.shortName(), getCustomerResponse.displayName());
    }

    public static LogInstitution fromCristinId(URI topLevelOrgCristinId) {
        return new LogInstitution(null, topLevelOrgCristinId, null, null);
    }
}
