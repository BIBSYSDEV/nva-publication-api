package no.unit.nva.expansion.restclients.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.ExpansionConfig;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstitutionResponse {

    private URI id;
    private List<SubUnit> subunits;

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public List<SubUnit> getSubunits() {
        return nonNull(subunits) ? subunits : emptyList();
    }

    public void setSubunits(List<SubUnit> subunits) {
        this.subunits = subunits;
    }

    public Set<URI> getOrganizationIds() {
        Set<URI> organizationIds = getSubunits().stream()
                .map(SubUnit::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        organizationIds.add(id);
        return organizationIds;
    }

    public static InstitutionResponse fromJson(String json) throws JsonProcessingException {
        return ExpansionConfig.objectMapper.readValue(json, InstitutionResponse.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubUnit {
        private URI id;

        public URI getId() {
            return id;
        }

        public void setId(URI id) {
            this.id = id;
        }
    }
}
