package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import nva.commons.core.SingletonCollector;

public enum RightsRetentionStrategyConfiguration {
    UNKNOWN("Unknown"),
    NULL_RIGHTS_RETENTION_STRATEGY("NullRightsRetentionStrategy"),
    RIGHTS_RETENTION_STRATEGY("RightsRetentionStrategy"),
    OVERRIDABLE_RIGHTS_RETENTION_STRATEGY("OverridableRightsRetentionStrategy");

    private final String value;

    RightsRetentionStrategyConfiguration(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RightsRetentionStrategyConfiguration fromValue(String value) {
        return Arrays.stream(RightsRetentionStrategyConfiguration.values())
                   .filter(enumValue -> enumValue.getValue().equalsIgnoreCase(value))
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow();
    }
}
