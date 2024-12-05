package no.unit.nva.publication.model.business.logentry;

import java.net.URI;
import no.unit.nva.clients.GetUserResponse;

/**
 * @param institution is a topLevelOrgCristinId of user Customer institution.
 */
public record LogUser(String userName, String givenName, String familyName, URI cristinId, URI institution) {

    public static LogUser fromGetUserResponse(GetUserResponse getUserResponse) {
        return new LogUser(getUserResponse.username(), getUserResponse.givenName(), getUserResponse.familyName(),
                           getUserResponse.cristinId(), getUserResponse.institutionCristinId());
    }

    public static LogUser fromUsername(String username) {
        return new LogUser(username, null, null, null, null);
    }
}
