package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(NullRightsRetentionStrategy.TYPE_NAME)
public final class NullRightsRetentionStrategy extends AbstractRightsRetentionStrategy {

    public static final String TYPE_NAME = "NullRightsRetentionStrategy";

    @JsonCreator
    private NullRightsRetentionStrategy(
        @JsonProperty(FIELD_NAME_CONFIGURED_TYPE) RightsRetentionStrategyConfiguration configuredType) {
        super(configuredType);
    }

    public static NullRightsRetentionStrategy create(RightsRetentionStrategyConfiguration configuredType) {
        return new NullRightsRetentionStrategy(configuredType);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(NullRightsRetentionStrategy.class.getName(), super.hashCode());

    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        return o instanceof NullRightsRetentionStrategy && super.equals(o);
    }
}
