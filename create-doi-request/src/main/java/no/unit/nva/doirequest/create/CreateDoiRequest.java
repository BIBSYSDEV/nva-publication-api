package no.unit.nva.doirequest.create;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;

public class CreateDoiRequest {

    @JsonAlias({"identifier", "publicationId"})
    private final SortableIdentifier resourceIdentifier;
    @JsonProperty("message")
    private final String message;

    @JsonCreator
    public CreateDoiRequest(@JsonProperty("resourceIdentifier") SortableIdentifier resourceIdentifier,
                            @JsonProperty("message") String message) {
        this.resourceIdentifier = resourceIdentifier;
        this.message = message;
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public String getMessage() {
        return message;
    }
}
