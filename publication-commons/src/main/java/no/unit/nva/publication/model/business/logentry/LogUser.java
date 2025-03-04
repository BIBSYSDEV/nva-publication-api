package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.UserDto;
import no.unit.nva.publication.model.business.User;

@JsonTypeName(LogUser.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogUser(@JsonAlias("userName") String username, String givenName, String familyName, URI cristinId,
                      LogOrganization onBehalfOf) {

    public static final String TYPE = "LogUser";

    public static LogUser fromResourceEvent(User username, URI topLevelOrgCristinId) {
        return new LogUser(username.toString(), null, null, null,
                           new LogOrganization(null, topLevelOrgCristinId, null, null));
    }

    public static LogUser create(UserDto getUserResponse, CustomerDto getCustomerResponse) {
        return new LogUser(getUserResponse.username(), getUserResponse.givenName(), getUserResponse.familyName(),
                           getUserResponse.cristinId(),
                           new LogOrganization(getCustomerResponse.id(), getCustomerResponse.cristinId(),
                                               getCustomerResponse.shortName(), getCustomerResponse.displayName()));
    }
}
