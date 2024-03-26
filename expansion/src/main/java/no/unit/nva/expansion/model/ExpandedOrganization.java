package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;

@JsonTypeName(ExpandedOrganization.TYPE)
public record ExpandedOrganization(@JsonProperty(ID_FIELD) URI id,
                                   @JsonProperty(IDENTIFIER_FIELD) String identifier,
                                   @JsonProperty(PART_OF_FIELD) List<URI> partOf) {

    public static final String TYPE = "Organization";
    public static final String ID_FIELD = "id";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String PART_OF_FIELD = "partOf";
}
