package no.unit.nva.publication.rightsretention;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.FunderRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

/**
 * Finds out which RRS a given file should have when a new RRS is set.
 */
public class RightsRetentionsValueFinder {

    public static final String ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE = "Illegal RightsRetentionStrategy on file ";
    private final RightsRetentionStrategyConfiguration configuredRightsRetention;
    private final PublicationPermissions permissionStrategy;
    private final String username;

    public RightsRetentionsValueFinder(CustomerApiRightsRetention configuredRrsOnCustomer,
                                       PublicationPermissions permissionStrategy,
                                       String username) {
        this.configuredRightsRetention = getConfigFromCustomerDto(configuredRrsOnCustomer);
        this.permissionStrategy = permissionStrategy;
        this.username = username;
    }

    public RightsRetentionStrategy getRightsRetentionStrategy(File file, Resource resource)
        throws BadRequestException, UnauthorizedException {
        var fileRightsRetention = file.getRightsRetentionStrategy();

        return rrsIsIrrelevant(file, resource)
                   ? NullRightsRetentionStrategy.create(configuredRightsRetention)
                   : getRrsForUnPublishedFile(file, fileRightsRetention);
    }

    private boolean rrsIsIrrelevant(File file, Resource resource) {
        return !PublisherVersion.ACCEPTED_VERSION.equals(file.getPublisherVersion())
               || file instanceof InternalFile
               || !isAcademicArticle(resource);
    }

    private static boolean isAcademicArticle(Resource resource) {
        return Optional.ofNullable(resource)
                   .map(Resource::getEntityDescription)
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(instance -> instance instanceof AcademicArticle)
                   .orElse(false);
    }

    @JacocoGenerated
    private static Set<RightsRetentionStrategyConfiguration> getAllowedConfigurations(RightsRetentionStrategy rrs) {
        return switch (rrs) {
            case OverriddenRightsRetentionStrategy strategy -> Set.of(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            case NullRightsRetentionStrategy strategy -> Set.of(NULL_RIGHTS_RETENTION_STRATEGY);
            case CustomerRightsRetentionStrategy strategy ->
                Set.of(RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            case FunderRightsRetentionStrategy strategy ->
                Set.of(NULL_RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            default -> throw new IllegalArgumentException("Unknown rrs type " + rrs);
        };
    }

    @JacocoGenerated
    private static RightsRetentionStrategyConfiguration getConfigFromCustomerDto(CustomerApiRightsRetention config) {
        return switch (config.getType()) {
            case "NullRightsRetentionStrategy" -> NULL_RIGHTS_RETENTION_STRATEGY;
            case "RightsRetentionStrategy" -> RIGHTS_RETENTION_STRATEGY;
            case "OverridableRightsRetentionStrategy" -> OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
            default -> throw new IllegalArgumentException("Unknown RightsRetentionStrategy type " + config.getType());
        };
    }

    private boolean isValid(RightsRetentionStrategy rrs) {
        return getAllowedConfigurations(rrs).contains(rrs.getConfiguredType())
               || isAllowedToOverrideRrs(rrs, permissionStrategy);
    }

    private boolean isAllowedToOverrideRrs(RightsRetentionStrategy rrs,
                                           PublicationPermissions permissionStrategy) {
        if (nonNull(permissionStrategy) && rrs instanceof OverriddenRightsRetentionStrategy) {
            return permissionStrategy.isPublishingCuratorOnPublication();
        }
        return false;
    }


    private RightsRetentionStrategy getRrsForUnPublishedFile(File file,
                                                             RightsRetentionStrategy fileRightsRetention)
        throws BadRequestException {
        var rrs = switch (fileRightsRetention) {
            case OverriddenRightsRetentionStrategy strategy ->
                OverriddenRightsRetentionStrategy.create(configuredRightsRetention, username);
            case NullRightsRetentionStrategy strategy -> NullRightsRetentionStrategy.create(configuredRightsRetention);
            case CustomerRightsRetentionStrategy strategy ->
                CustomerRightsRetentionStrategy.create(configuredRightsRetention);
            case FunderRightsRetentionStrategy strategy ->
                FunderRightsRetentionStrategy.create(configuredRightsRetention);
            default ->
                throw new IllegalArgumentException("Unknown RightsRetentionStrategy type " + fileRightsRetention);
        };
        if (!isValid(rrs)) {
            throw new BadRequestException(ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE + file.getIdentifier());
        }
        return rrs;
    }
}
