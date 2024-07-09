package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonValue;

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
}
