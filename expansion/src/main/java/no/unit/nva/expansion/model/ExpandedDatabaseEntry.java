package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.ID_NAMESPACE;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Publication", value = ExpandedResource.class),
    @JsonSubTypes.Type(name = "DoiRequest", value = ExpandedDoiRequest.class),
    @JsonSubTypes.Type(name = "Message", value = ExpandedMessage.class),
})
public interface ExpandedDatabaseEntry extends JsonSerializable {

    @JacocoGenerated
    @Override
    default String toJsonString() {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    SortableIdentifier fetchIdentifier();

    default URI fetchId() {
        return new UriWrapper(ID_NAMESPACE).addChild(this.fetchIdentifier().toString()).getUri();
    }

}
