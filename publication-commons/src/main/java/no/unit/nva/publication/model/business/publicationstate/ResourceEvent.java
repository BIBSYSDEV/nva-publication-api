package no.unit.nva.publication.model.business.publicationstate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.model.business.User;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(CreatedResourceEvent.class), @JsonSubTypes.Type(PublishedResourceEvent.class),
    @JsonSubTypes.Type(UnpublishedResourceEvent.class), @JsonSubTypes.Type(DeletedResourceEvent.class)})
public interface ResourceEvent {

    Instant date();

    User user();

    /**
     * @return id of the top level cristin organizations
     */
    URI institution();
}
