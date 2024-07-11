package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(AssociatedLink.TYPE_NAME)
public class AssociatedLink implements AssociatedArtifact {

    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String TYPE_NAME = "AssociatedLink";
    @JsonProperty(ID_FIELD)
    private final URI id;
    @JsonProperty(NAME_FIELD)
    private final String name;
    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;
    

    @JsonCreator
    public AssociatedLink(@JsonProperty(ID_FIELD) URI id,
                          @JsonProperty(NAME_FIELD) String name,
                          @JsonProperty(DESCRIPTION_FIELD) String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public URI getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssociatedLink)) {
            return false;
        }
        AssociatedLink that = (AssociatedLink) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getName(), getDescription());
    }

    public enum RelationType {
        GENERIC_LINKED_RESOURCE("GenericLinkedResource");

        private final String type;

        RelationType(String type) {

            this.type = type;
        }

        @JsonValue
        public String getType() {
            return type;
        }

        @JsonCreator
        public RelationType lookup(String candidate) {
            return Arrays.stream(RelationType.values())
                       .filter(item -> item.getType().equals(candidate))
                       .collect(SingletonCollector.collect());
        }
    }
}
