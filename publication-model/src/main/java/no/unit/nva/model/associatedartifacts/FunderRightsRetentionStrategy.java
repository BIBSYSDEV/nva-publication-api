package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(FunderRightsRetentionStrategy.TYPE_NAME)
public final class FunderRightsRetentionStrategy extends AbstractRightsRetentionStrategy {

    public static final String TYPE_NAME = "FunderRightsRetentionStrategy";

    @JsonCreator
    private FunderRightsRetentionStrategy(
        @JsonProperty(FIELD_NAME_CONFIGURED_TYPE) RightsRetentionStrategyConfiguration configuredType) {
        super(configuredType);
    }

    public static FunderRightsRetentionStrategy create(RightsRetentionStrategyConfiguration configuredType) {
        return new FunderRightsRetentionStrategy(configuredType);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(FunderRightsRetentionStrategy.class.getName(), super.hashCode());

    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        return o instanceof FunderRightsRetentionStrategy && super.equals(o);
    }
}
