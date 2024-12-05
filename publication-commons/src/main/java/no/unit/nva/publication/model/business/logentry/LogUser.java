package no.unit.nva.publication.model.business.logentry;

import java.net.URI;
import no.unit.nva.clients.GetUserResponse;

public record LogUser(String userName, String givenName, String familyName, URI cristinId, URI topLevelOrgCristinId) {

    public static LogUser fromGetUserResponse(GetUserResponse getUserResponse) {
        return new LogUser(getUserResponse.username(), getUserResponse.givenName(), getUserResponse.familyName(),
                           getUserResponse.cristinId(), getUserResponse.institutionCristinId());
    }

    public static LogUser fromUsername(String username) {
        return new LogUser(username, null, null, null, null);
    }
}
