package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Map;
import no.unit.nva.clients.cristin.CristinOrganizationDto;

@JsonTypeName(LogOrganization.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record LogOrganization(URI id, String acronym, Map<String, String> labels) implements LogAgent {

    public static final String TYPE = "Organization";

    public static LogOrganization fromCristinId(URI topLevelOrgCristinId) {
        return new LogOrganization(topLevelOrgCristinId, null, null);
    }

    public static LogOrganization fromCristinOrganization(CristinOrganizationDto cristinOrganizationDto) {
        return new LogOrganization(cristinOrganizationDto.id(), cristinOrganizationDto.acronym(),
                            cristinOrganizationDto.labels());
    }
}
