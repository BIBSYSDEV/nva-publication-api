package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.clients.cristin.CristinOrganizationDto;
import no.unit.nva.clients.cristin.CristinPersonDto;
import no.unit.nva.clients.cristin.TypedValue;
import no.unit.nva.publication.model.business.User;

@JsonTypeName(LogUser.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogUser(@JsonAlias("userName") String username, URI id, String givenName, String familyName,
                      String preferredFirstName, String preferredLastName,
                      LogOrganization onBehalfOf) implements LogAgent {

    public static final String TYPE = "Person";
    public static final String PREFERRED_FIRST_NAME = "PreferredFirstName";
    public static final String PREFERRED_LAST_NAME = "PreferredLastName";

    public static LogUser fromResourceEvent(User username, URI topLevelOrgCristinId) {
        return new LogUser(username.toString(), null, null, null, null, null,
                           new LogOrganization(topLevelOrgCristinId, null, null));
    }

    public static LogUser create(CristinPersonDto cristinPersonDto, CristinOrganizationDto cristinOrganizationDto) {
        return new LogUser(null, cristinPersonDto.id(), cristinPersonDto.firstName().orElse(null),
                           cristinPersonDto.lastName().orElse(null),
                           getName(cristinPersonDto, PREFERRED_FIRST_NAME),
                           getName(cristinPersonDto, PREFERRED_LAST_NAME),
                           LogOrganization.fromCristinOrganization(cristinOrganizationDto));
    }

    private static String getName(CristinPersonDto cristinPersonDto, String nameType) {
        return cristinPersonDto.names()
                   .stream()
                   .filter(typedValue -> nameType.equals(typedValue.type()))
                   .map(TypedValue::value)
                   .findFirst()
                   .orElse(null);
    }
}
