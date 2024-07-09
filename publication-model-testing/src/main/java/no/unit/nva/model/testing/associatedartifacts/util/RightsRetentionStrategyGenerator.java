package no.unit.nva.model.testing.associatedartifacts.util;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.UNKNOWN;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.util.Random;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.FunderRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;

public final class RightsRetentionStrategyGenerator {

    private RightsRetentionStrategyGenerator() {

    }

    /**
     * Generates and returns a random RightsRetentionStrategy. The method randomly selects one of the three implemented
     * types of RightsRetentionStrategy: CustomerRightsRetentionStrategy, OverriddenRightsRetentionStrategy, or
     * NullRightsRetentionStrategy.
     *
     * @return A randomly selected RightsRetentionStrategy object.
     */
    public static RightsRetentionStrategy randomRightsRetentionStrategy() {
        RightsRetentionStrategy[] strategies = {
            CustomerRightsRetentionStrategy.create(UNKNOWN),
            FunderRightsRetentionStrategy.create(UNKNOWN),
            OverriddenRightsRetentionStrategy.create(UNKNOWN, randomString()),
            NullRightsRetentionStrategy.create(UNKNOWN)
        };

        return strategies[new Random().nextInt(strategies.length)];
    }
}