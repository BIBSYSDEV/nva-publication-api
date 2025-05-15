package no.unit.nva.model;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CuratingInstitution(URI id, Set<URI> contributorCristinIds) {

    @JsonCreator
    public static CuratingInstitution create(@JsonProperty("id") URI id,
                                             @JsonProperty("contributorCristinIds") Set<URI> contributorCristinIds) {
        return new CuratingInstitution(id, getContributorCristinIds(contributorCristinIds));
    }

    @JsonCreator
    public static CuratingInstitution create(String id) {
        return new CuratingInstitution(URI.create(id), Collections.emptySet());
    }

    @Override
    public Set<URI> contributorCristinIds() {
        return nonNull(contributorCristinIds) ? contributorCristinIds : Collections.emptySet();
    }

    private static Set<URI> getContributorCristinIds(Set<URI> contributorCristinIds) {
        return nonNull(contributorCristinIds) && !contributorCristinIds.isEmpty() ? contributorCristinIds
                   : Collections.emptySet();
    }
}
