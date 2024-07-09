package no.unit.nva.model.associatedartifacts;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * The AbstractRightsRetentionStrategy abstract class represents the strategy for rights retention. It makes use of
 * Jackson annotations to handle JSON serialization and deserialization into the correct implementing types:
 * CustomerRightsRetentionStrategy, OverriddenRightsRetentionStrategy and NullRightsRetentionStrategy.
 */
abstract class AbstractRightsRetentionStrategy implements RightsRetentionStrategy {

    protected static final String FIELD_NAME_CONFIGURED_TYPE = "configuredType";

    @JsonProperty(FIELD_NAME_CONFIGURED_TYPE)
    private RightsRetentionStrategyConfiguration configuredType;

    AbstractRightsRetentionStrategy(RightsRetentionStrategyConfiguration configuredType) {
        this.configuredType = nonNull(configuredType) ? configuredType
                                  : RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
    }

    @Override
    public RightsRetentionStrategyConfiguration getConfiguredType() {
        return configuredType;
    }

    public void setConfiguredType(RightsRetentionStrategyConfiguration configuredType) {
        this.configuredType = nonNull(configuredType) ? configuredType
                                  : RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRightsRetentionStrategy that = (AbstractRightsRetentionStrategy) o;
        return getConfiguredType() == that.getConfiguredType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConfiguredType());
    }
}
