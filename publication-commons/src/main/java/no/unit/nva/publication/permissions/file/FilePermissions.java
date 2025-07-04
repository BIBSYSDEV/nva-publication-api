package no.unit.nva.publication.permissions.file;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.deny.ClaimedChannelFileDenyStrategy;
import no.unit.nva.publication.permissions.file.deny.EmbargoDownloadDenyStrategy;
import no.unit.nva.publication.permissions.file.deny.DegreeEmbargoWriteDeleteDenyStrategy;
import no.unit.nva.publication.permissions.file.deny.DegreeWriteDeleteDenyStrategy;
import no.unit.nva.publication.permissions.file.deny.EmbargoWriteDeleteDenyStrategy;
import no.unit.nva.publication.permissions.file.deny.HiddenFileDenyStrategy;
import no.unit.nva.publication.permissions.file.deny.UploadedFileDenyStrategy;
import no.unit.nva.publication.permissions.file.grant.ContributorFileGrantStrategy;
import no.unit.nva.publication.permissions.file.grant.DegreeCuratorFileGrantStrategy;
import no.unit.nva.publication.permissions.file.grant.DegreeEmbargoCuratorFileGrantStrategy;
import no.unit.nva.publication.permissions.file.grant.EditorFileGrantStrategy;
import no.unit.nva.publication.permissions.file.grant.EmbargoCuratorFileGrantStrategy;
import no.unit.nva.publication.permissions.file.grant.EveryoneGrantStrategy;
import no.unit.nva.publication.permissions.file.grant.CuratorFileGrantStrategy;
import no.unit.nva.publication.permissions.file.grant.ExternalClientGrantStrategy;
import no.unit.nva.publication.permissions.file.grant.FileOwnerGrantStrategy;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilePermissions {

    private static final Logger logger = LoggerFactory.getLogger(FilePermissions.class);
    public static final String COMMA_DELIMITER = ", ";
    private final Set<FileGrantStrategy> grantStrategies;
    private final Set<FileDenyStrategy> denyStrategies;
    private final UserInstance userInstance;
    private final FileEntry file;

    public FilePermissions(
        FileEntry file,
        UserInstance userInstance,
        Resource resource) {
        this.userInstance = userInstance;
        this.file = file;
        this.grantStrategies = Set.of(
            new EveryoneGrantStrategy(file, userInstance, resource),
            new CuratorFileGrantStrategy(file, userInstance, resource),
            new FileOwnerGrantStrategy(file, userInstance, resource),
            new ContributorFileGrantStrategy(file, userInstance, resource),
            new ExternalClientGrantStrategy(file, userInstance, resource),
            new DegreeEmbargoCuratorFileGrantStrategy(file, userInstance, resource),
            new DegreeCuratorFileGrantStrategy(file, userInstance, resource),
            new EmbargoCuratorFileGrantStrategy(file, userInstance, resource),
            new EditorFileGrantStrategy(file, userInstance, resource)
        );
        this.denyStrategies = Set.of(
            new HiddenFileDenyStrategy(file, userInstance, resource),
            new UploadedFileDenyStrategy(file, userInstance, resource),
            new EmbargoDownloadDenyStrategy(file, userInstance, resource),
            new EmbargoWriteDeleteDenyStrategy(file, userInstance, resource),
            new DegreeWriteDeleteDenyStrategy(file, userInstance, resource),
            new DegreeEmbargoWriteDeleteDenyStrategy(file, userInstance, resource),
            new ClaimedChannelFileDenyStrategy(file, userInstance, resource)
        );
    }

    public static FilePermissions create(FileEntry file, UserInstance userInstance, Resource resource) {
        return new FilePermissions(file, userInstance, resource);
    }

    public boolean allowsAction(FileOperation permission) {
        return !findAllowances(permission).isEmpty()
               && findDenials(permission).isEmpty();
    }

    public Set<FileOperation> getAllAllowedActions() {
        return Arrays.stream(FileOperation.values())
                   .filter(this::allowsAction)
                   .collect(Collectors.toSet());
    }

    public void authorize(FileOperation requestedPermission) throws UnauthorizedException {
        validateDenyStrategiesRestrictions(requestedPermission);
        validateGrantStrategies(requestedPermission);
    }

    private List<FileGrantStrategy> findAllowances(FileOperation permission) {
        return grantStrategies.stream()
                   .filter(strategy -> strategy.allowsAction(permission))
                   .toList();
    }

    private List<FileDenyStrategy> findDenials(FileOperation permission) {
        return denyStrategies.stream()
                   .filter(strategy -> strategy.deniesAction(permission))
                   .toList();
    }

    private void validateDenyStrategiesRestrictions(FileOperation requestedPermission)
        throws UnauthorizedException {
        var strategies = findDenials(requestedPermission).stream()
                                     .map(FileDenyStrategy::getClass)
                                     .map(Class::getSimpleName)
                                     .toList();

        if (!strategies.isEmpty()) {
            logger.info("User {} was denied access {} on file {} from strategies {}",
                        userInstance.getUsername(),
                        requestedPermission,
                        file.getIdentifier(),
                        String.join(COMMA_DELIMITER, strategies));

            throw new UnauthorizedException(formatUnauthorizedMessage(requestedPermission));
        }
    }

    private void validateGrantStrategies(FileOperation requestedPermission) throws UnauthorizedException {
        var strategies = findAllowances(requestedPermission).stream()
                                  .map(FileGrantStrategy::getClass)
                                  .map(Class::getSimpleName)
                                  .toList();

        if (strategies.isEmpty()) {
            throw new UnauthorizedException(formatUnauthorizedMessage(requestedPermission));
        }

        logger.info("User {} was allowed {} on file {} from strategies {}",
                    userInstance.getUsername(),
                    requestedPermission,
                    file.getIdentifier(),
                    String.join(COMMA_DELIMITER, strategies));
    }

    private String formatUnauthorizedMessage(FileOperation requestedPermission) {
        return String.format("Unauthorized: %s is not allowed to perform %s on %s", userInstance.getUsername(),
                             requestedPermission, file.getIdentifier());
    }

}

