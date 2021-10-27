package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        return subunits;
    }

    public void setSubunits(List<SubUnit> subunits) {
        this.subunits = subunits;
    }

    public Set<URI> getOrganizationIds() {
        Set<URI> organizationIds = subunits.stream()
                .map(SubUnit::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        organizationIds.add(id);
        return organizationIds;
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
