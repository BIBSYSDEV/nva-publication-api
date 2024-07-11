package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @Type(name = CustomerRightsRetentionStrategy.TYPE_NAME, value = CustomerRightsRetentionStrategy.class),
    @Type(name = OverriddenRightsRetentionStrategy.TYPE_NAME, value = OverriddenRightsRetentionStrategy.class),
    @Type(name = NullRightsRetentionStrategy.TYPE_NAME, value = NullRightsRetentionStrategy.class),
    @Type(name = FunderRightsRetentionStrategy.TYPE_NAME, value = FunderRightsRetentionStrategy.class)
})
public interface RightsRetentionStrategy {
    RightsRetentionStrategyConfiguration getConfiguredType();
}
