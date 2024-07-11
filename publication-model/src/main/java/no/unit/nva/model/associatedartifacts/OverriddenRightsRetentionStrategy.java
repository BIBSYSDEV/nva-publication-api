package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(OverriddenRightsRetentionStrategy.TYPE_NAME)
public final class OverriddenRightsRetentionStrategy extends AbstractRightsRetentionStrategy {

    private static final String FIELD_NAME_OVERRIDDEN_BY = "overriddenBy";
    public static final String TYPE_NAME = "OverriddenRightsRetentionStrategy";

    private String overriddenBy;

    @JsonCreator
    private OverriddenRightsRetentionStrategy(
        @JsonProperty(FIELD_NAME_CONFIGURED_TYPE) RightsRetentionStrategyConfiguration configuredType,
        @JsonProperty(FIELD_NAME_OVERRIDDEN_BY) String overriddenBy
    ) {
        super(configuredType);
        this.overriddenBy = overriddenBy;
    }

    public static OverriddenRightsRetentionStrategy create(RightsRetentionStrategyConfiguration configuredType,
                                                           String overriddenBy) {
        return new OverriddenRightsRetentionStrategy(configuredType, overriddenBy);
    }

    public String getOverriddenBy() {
        return overriddenBy;
    }

    public void setOverriddenBy(String overriddenBy) {
        this.overriddenBy = overriddenBy;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(OverriddenRightsRetentionStrategy.class.getName(), overriddenBy, super.hashCode());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        return o instanceof OverriddenRightsRetentionStrategy && super.equals(o);
    }
}
