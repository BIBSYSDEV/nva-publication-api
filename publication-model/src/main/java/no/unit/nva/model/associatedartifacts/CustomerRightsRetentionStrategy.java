package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(CustomerRightsRetentionStrategy.TYPE_NAME)
public final class CustomerRightsRetentionStrategy extends AbstractRightsRetentionStrategy {

    public static final String TYPE_NAME = "CustomerRightsRetentionStrategy";

    @JsonCreator
    private CustomerRightsRetentionStrategy(
        @JsonProperty(FIELD_NAME_CONFIGURED_TYPE) RightsRetentionStrategyConfiguration configuredType) {
        super(configuredType);
    }

    public static CustomerRightsRetentionStrategy create(RightsRetentionStrategyConfiguration configuredType) {
        return new CustomerRightsRetentionStrategy(configuredType);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        return o instanceof CustomerRightsRetentionStrategy && super.equals(o);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(CustomerRightsRetentionStrategy.class.getName(), super.hashCode());
    }
}
