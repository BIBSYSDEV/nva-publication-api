package no.unit.nva.publication.rightsretention;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.FileOperation.WRITE_METADATA;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
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
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissions;
import nva.commons.apigateway.exceptions.BadRequestException;

public class FileRightsRetentionService {

    public static final String ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE = "Invalid rights retention strategy";

    private final CustomerApiRightsRetention customerApiRightsRetention;
    private final UserInstance userInstance;

    public FileRightsRetentionService(CustomerApiRightsRetention customerApiRightsRetention,
                                      UserInstance userInstance) {
        this.customerApiRightsRetention = customerApiRightsRetention;
        this.userInstance = userInstance;
    }

    /**
     * Apply rights retention strategy to files when updating existing publication
     */
    public void applyRightsRetention(Resource newImage, Resource oldImage)
        throws BadRequestException {
        var existingFileEntries = getFileEntries(oldImage);
        for (var file : getFiles(newImage)) {
            var existingFileEntry = existingFileEntries.getOrDefault(
                new SortableIdentifier(file.getIdentifier().toString()), null);
            applyRightsRetentionToExistingFile(file, existingFileEntry, newImage);
        }
    }

    /**
     * Apply rights retention strategy to all files in a new publication
     */
    public void applyRightsRetention(Resource resource) throws BadRequestException {
        applyDefaultStrategyToAllFiles(resource);
    }

    /**
     * Apply strategy to all files in a new publication - processes file's requested strategy
     */
    private void applyDefaultStrategyToAllFiles(Resource resource) throws BadRequestException {
        for (var file : getFiles(resource)) {
            if (isRightsRetentionRelevant(file, resource)) {
                // Process what the file is requesting, like the original logic
                var requestedStrategy = file.getRightsRetentionStrategy();
                var validatedStrategy = createValidatedStrategy(requestedStrategy, null, resource);
                file.setRightsRetentionStrategy(validatedStrategy);
            } else {
                file.setRightsRetentionStrategy(createNullStrategy());
            }
        }
    }

    /**
     * Apply rights retention to a file in an existing publication
     */
    private void applyRightsRetentionToExistingFile(File file, FileEntry existingFile, Resource resource)
        throws BadRequestException {

        // Rule 1: Skip if RRS is not relevant for this file
        if (!isRightsRetentionRelevant(file, resource)) {
            file.setRightsRetentionStrategy(createNullStrategy());
            return;
        }

        // Rule 2: New file in existing publication - process requested strategy
        if (isNull(existingFile)) {
            file.setRightsRetentionStrategy(determineDefaultStrategy(file, resource));
            return;
        }

        // Rule 3: Existing file - preserve or update based on changes
        var requestedStrategy = file.getRightsRetentionStrategy();
        var existingStrategy = existingFile.getFile().getRightsRetentionStrategy();

        if (strategyTypeChanged(requestedStrategy, existingStrategy)) {
            // Validate and create the requested strategy
            var validatedStrategy = createValidatedStrategy(requestedStrategy, existingFile, resource);
            file.setRightsRetentionStrategy(validatedStrategy);
        } else {
            // Keep existing strategy
            file.setRightsRetentionStrategy(existingStrategy);
        }
    }

    /**
     * Simple rule: RRS only applies to accepted academic articles that aren't internal
     */
    private boolean isRightsRetentionRelevant(File file, Resource resource) {
        return PublisherVersion.ACCEPTED_VERSION.equals(file.getPublisherVersion())
               && !(file instanceof InternalFile)
               && isAcademicArticle(resource);
    }

    private boolean isAcademicArticle(Resource resource) {
        return Optional.ofNullable(resource)
                   .map(Resource::getEntityDescription)
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .filter(AcademicArticle.class::isInstance)
                   .isPresent();
    }

    private RightsRetentionStrategy determineDefaultStrategy(File file, Resource resource)
        throws BadRequestException {
        return createValidatedStrategy(file.getRightsRetentionStrategy(), null, resource);
    }

    private boolean strategyTypeChanged(RightsRetentionStrategy newStrategy, RightsRetentionStrategy oldStrategy) {
        return !newStrategy.getClass().equals(oldStrategy.getClass());
    }

    private RightsRetentionStrategy createValidatedStrategy(RightsRetentionStrategy requestedStrategy,
                                                            FileEntry existingFile, Resource resource)
        throws BadRequestException {

        // Create the strategy based on what the file is requesting
        var rrs = switch (requestedStrategy) {
            case OverriddenRightsRetentionStrategy ignored ->
                OverriddenRightsRetentionStrategy.create(getConfig(), userInstance.getUsername());
            case NullRightsRetentionStrategy ignored -> createNullStrategy();
            case CustomerRightsRetentionStrategy ignored -> createCustomerStrategy();
            case FunderRightsRetentionStrategy ignored -> createFunderStrategy();
            default -> throw new IllegalArgumentException("Unknown RightsRetentionStrategy type " + requestedStrategy);
        };

        // Validate if matches customer config OR user can override
        if (!isValidStrategy(rrs, existingFile, resource)) {
            throw new BadRequestException(ILLEGAL_RIGHTS_RETENTION_STRATEGY_ON_FILE);
        }

        return rrs;
    }

    private boolean isValidStrategy(RightsRetentionStrategy rrs, FileEntry existingFile, Resource resource) {
        return isValidStrategyForCustomerConfig(rrs) || (rrs instanceof OverriddenRightsRetentionStrategy
                                                         && canOverrideStrategy(existingFile, resource));
    }

    private boolean canOverrideStrategy(FileEntry existingFile, Resource resource) {
        return nonNull(existingFile) && new FilePermissions(existingFile, userInstance, resource).allowsAction(
            WRITE_METADATA);
    }

    private boolean isValidStrategyForCustomerConfig(RightsRetentionStrategy strategy) {
        var allowedConfigurations = getAllowedConfigurationsForStrategy(strategy);
        return allowedConfigurations.contains(strategy.getConfiguredType());
    }

    private Set<RightsRetentionStrategyConfiguration> getAllowedConfigurationsForStrategy(RightsRetentionStrategy rrs) {
        return switch (rrs) {
            case OverriddenRightsRetentionStrategy ignored -> Set.of(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            case NullRightsRetentionStrategy ignored -> Set.of(NULL_RIGHTS_RETENTION_STRATEGY);
            case CustomerRightsRetentionStrategy ignored ->
                Set.of(RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            case FunderRightsRetentionStrategy ignored ->
                Set.of(NULL_RIGHTS_RETENTION_STRATEGY, OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
            default -> throw new IllegalArgumentException("Unknown rrs type " + rrs);
        };
    }

    // Helper methods for strategy creation
    private NullRightsRetentionStrategy createNullStrategy() {
        return NullRightsRetentionStrategy.create(getConfig());
    }

    private CustomerRightsRetentionStrategy createCustomerStrategy() {
        return CustomerRightsRetentionStrategy.create(getConfig());
    }

    private FunderRightsRetentionStrategy createFunderStrategy() {
        return FunderRightsRetentionStrategy.create(getConfig());
    }

    private RightsRetentionStrategyConfiguration getConfig() {
        return RightsRetentionStrategyConfiguration.fromValue(customerApiRightsRetention.getType());
    }

    private List<File> getFiles(Resource resource) {
        return resource.getAssociatedArtifacts().stream().filter(File.class::isInstance).map(File.class::cast).toList();
    }

    private Map<SortableIdentifier, FileEntry> getFileEntries(Resource resource) {
        return resource.getFileEntries()
                   .stream()
                   .collect(Collectors.toMap(FileEntry::getIdentifier, Function.identity()));
    }
}