package no.unit.nva.model;

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
                                             @JsonProperty("contributorCristinIds") List<URI> curatedContributors) {
        return new CuratingInstitution(id, curatedContributors != null ? curatedContributors : Collections.emptyList());
    }

    @JsonCreator
    public static CuratingInstitution create(String id) {
        return new CuratingInstitution(URI.create(id), Collections.emptyList());
    }
}
