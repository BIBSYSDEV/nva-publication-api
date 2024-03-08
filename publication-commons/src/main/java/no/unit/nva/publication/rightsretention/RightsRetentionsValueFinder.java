package no.unit.nva.publication.rightsretention;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Set;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.FunderRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import nva.commons.apigateway.exceptions.BadRequestException;

/**
 * Finds out which RRS a given file should have when a new RRS is set
 */
public class RightsRetentionsValueFinder {

    public static final String ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE = "Illegal RightsRetentionStrategy on file ";

    public static RightsRetentionStrategy getRightsRetentionStrategy(CustomerApiRightsRetention configuredRrsOnCustomer,
                                                                     Publication publication, File file,
                                                                     String username)
        throws BadRequestException {
        var configuredRightsRetention = getConfigFromCustomerDto(configuredRrsOnCustomer);
        var fileRightsRetention = file.getRightsRetentionStrategy();

        return rrsIsIrrelevant(file, publication)
                   ? NullRightsRetentionStrategy.create(configuredRightsRetention)
                   : getRrsForUnPublishedFile(file, configuredRightsRetention, fileRightsRetention, username);
    }

    private static boolean rrsIsIrrelevant(File file, Publication publication) {
        return file.isPublisherAuthority()
               || file instanceof AdministrativeAgreement
               || !isAcademicArticle(publication);
    }

    private static boolean isAcademicArticle(Publication publication) {
        return (publication.getEntityDescription().getReference().getPublicationInstance() instanceof AcademicArticle);
    }

    private static RightsRetentionStrategy getRrsForUnPublishedFile(File file, RightsRetentionStrategyConfiguration rrsConfig,
                                                                    RightsRetentionStrategy fileRightsRetention,
                                                                    String username) throws BadRequestException {
        var rrs = switch (fileRightsRetention) {
            case OverriddenRightsRetentionStrategy strategy -> OverriddenRightsRetentionStrategy.create(rrsConfig, username);
            case NullRightsRetentionStrategy strategy -> NullRightsRetentionStrategy.create(rrsConfig);
            case CustomerRightsRetentionStrategy strategy -> CustomerRightsRetentionStrategy.create(rrsConfig);
            case FunderRightsRetentionStrategy strategy -> FunderRightsRetentionStrategy.create(rrsConfig);
            default -> throw new IllegalArgumentException("Unknown RightsRetentionStrategy type "+ fileRightsRetention);
        };
        if (!isValid(rrs)) {
            throw new BadRequestException(ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE + file.getIdentifier());
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

    private static RightsRetentionStrategyConfiguration getConfigFromCustomerDto(CustomerApiRightsRetention config) {
        return switch (config.getType()) {
            case "NullRightsRetentionStrategy" -> NULL_RIGHTS_RETENTION_STRATEGY;
            case "RightsRetentionStrategy" -> RIGHTS_RETENTION_STRATEGY;
            case "OverridableRightsRetentionStrategy" -> OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
            default -> throw new IllegalArgumentException("Unknown RightsRetentionStrategy type "+ config.getType());
        };
    }

}
