package no.unit.nva.publication.model.business.publicationstate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.model.business.User;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(CreatedState.class),
    @JsonSubTypes.Type(PublishedState.class),
    @JsonSubTypes.Type(UnpublishedState.class),
    @JsonSubTypes.Type(DeletedState.class)
})
public interface State {

    Instant date();

    User user();

    URI institution();
}
