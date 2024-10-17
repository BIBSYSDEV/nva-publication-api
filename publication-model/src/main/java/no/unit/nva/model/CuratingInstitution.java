package no.unit.nva.model;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CuratingInstitution(URI id, List<URI> contributorCristinIds) {

    @JsonCreator
    public static CuratingInstitution create(@JsonProperty("id") URI id,
                                             @JsonProperty("contributorCristinIds") List<URI> contributorCristinIds) {
        return new CuratingInstitution(id, getContributorCristinIds(contributorCristinIds));
    }

    @JsonCreator
    public static CuratingInstitution create(String id) {
        return new CuratingInstitution(URI.create(id), Collections.emptyList());
    }

    @Override
    public List<URI> contributorCristinIds() {
        return nonNull(contributorCristinIds) ? contributorCristinIds : Collections.emptyList();
    }

    private static List<URI> getContributorCristinIds(List<URI> contributorCristinIds) {
        return nonNull(contributorCristinIds) && !contributorCristinIds.isEmpty() ? contributorCristinIds
                   : Collections.emptyList();
    }
}
