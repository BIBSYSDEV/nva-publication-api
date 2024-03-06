package no.unit.nva.publication.rightsretention;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Set;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.FunderRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.Customer;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;

public class RightsRetentionsUtils {
    public static RightsRetentionStrategy getRightsRetentionStrategy(Customer customer, File file,
                                                                     RequestInfo requestInfo)
        throws BadRequestException {
        var configuredRightsRetention = getConfigFromCustomerDto(customer);
        var overridedRightsRetention = file.getRightsRetentionStrategy();
        var username = attempt(() -> requestInfo.getUserName()).orElseThrow();

        return file.isPublisherAuthority()
                   ? getRrsForPublishedFile(configuredRightsRetention)
                   : getRrsForUnPublishedFile(configuredRightsRetention, overridedRightsRetention, username);
    }

    private static RightsRetentionStrategy getRrsForPublishedFile(RightsRetentionStrategyConfiguration config) {
        return NullRightsRetentionStrategy.create(config);
    }

    private static RightsRetentionStrategy getRrsForUnPublishedFile(RightsRetentionStrategyConfiguration configuredRightsRetention,
                                                                    RightsRetentionStrategy overridedRightsRetention,
                                                                    String username) throws BadRequestException {
        var rrs = switch (overridedRightsRetention) {
            case OverriddenRightsRetentionStrategy strategy -> OverriddenRightsRetentionStrategy.create(configuredRightsRetention, username);
            case NullRightsRetentionStrategy strategy -> NullRightsRetentionStrategy.create(configuredRightsRetention);
            case CustomerRightsRetentionStrategy strategy -> CustomerRightsRetentionStrategy.create(configuredRightsRetention);
            case FunderRightsRetentionStrategy strategy -> FunderRightsRetentionStrategy.create(configuredRightsRetention);
            default -> throw new IllegalArgumentException("Unknown RightsRetentionStrategy type "+ overridedRightsRetention);
        };
        if (!isValid(rrs)) {
            throw new BadRequestException("Illegal RightsRetentionStrategy on file");
        }
        return rrs;
    }

    private static boolean isValid(RightsRetentionStrategy rrs) {
        return getAllowedConfigurations(rrs).contains(rrs.getConfiguredType());
    }

    private static Set<RightsRetentionStrategyConfiguration> getAllowedConfigurations(RightsRetentionStrategy rrs) {
        return switch (rrs) {
            case OverriddenRightsRetentionStrategy strategy -> Set.of(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            case NullRightsRetentionStrategy strategy -> Set.of(NULL_RIGHTS_RETENTION_STRATEGY);
            case CustomerRightsRetentionStrategy strategy -> Set.of(RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            case FunderRightsRetentionStrategy strategy -> Set.of(NULL_RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            default -> throw new IllegalArgumentException("Unknown rrs type "+ rrs);
        };
    }

    private static RightsRetentionStrategyConfiguration getConfigFromCustomerDto(Customer customer) {
        var rrs = customer.getRightsRetentionStrategy().getType();
        return switch (rrs) {
            case "NullRightsRetentionStrategy" -> NULL_RIGHTS_RETENTION_STRATEGY;
            case "RightsRetentionStrategy" -> RIGHTS_RETENTION_STRATEGY;
            case "OverridableRightsRetentionStrategy" -> OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
            default -> throw new IllegalArgumentException("Unknown RightsRetentionStrategy type "+ rrs);
        };
    }

}
