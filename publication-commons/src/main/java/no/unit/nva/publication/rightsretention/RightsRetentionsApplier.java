package no.unit.nva.publication.rightsretention;

import com.google.common.collect.Lists;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class RightsRetentionsApplier {

    private final Optional<Resource> existingResource;
    private final Resource updatedResource;
    private final CustomerApiRightsRetention configuredRrsOnCustomer;
    private final String actingUser;
    private final PublicationPermissions permissionStrategy;

    private RightsRetentionsApplier(Optional<Resource> existingResource,
                                    Resource updatedResource,
                                    CustomerApiRightsRetention configuredRrsOnCustomer,
                                    String actingUser,
                                    PublicationPermissions permissionStrategy) {
        this.existingResource = existingResource;
        this.updatedResource = updatedResource;
        this.configuredRrsOnCustomer = configuredRrsOnCustomer;
        this.actingUser = actingUser;
        this.permissionStrategy = permissionStrategy;
    }

    public void handle() throws BadRequestException, UnauthorizedException {
        if (existingResource.isEmpty() || isChangedPublicationType(updatedResource, existingResource.get())) {
            setRrsOnAllFiles(updatedResource, configuredRrsOnCustomer, actingUser);
        } else {
            setRrsOnModifiedFiles(updatedResource,
                                  existingResource.get(),
                                  configuredRrsOnCustomer,
                                  actingUser);
        }
    }

    private static boolean isChangedPublicationType(Resource resourceUpdate, Resource existingResource) {
        var newInstance = Optional.ofNullable(resourceUpdate)
                              .map(Resource::getEntityDescription)
                              .map(EntityDescription::getReference)
                              .map(Reference::getPublicationInstance);
        var existingInstance = Optional.ofNullable(existingResource)
                                   .map(Resource::getEntityDescription)
                                   .map(EntityDescription::getReference)
                                   .map(Reference::getPublicationInstance);
        var isUpdatingNewPublication = existingInstance.isEmpty() && newInstance.isPresent();
        var isUpdatingExistingPublication = existingInstance.isPresent()
                                            && newInstance.isPresent()
                                            && !newInstance.get().getClass().equals(existingInstance.get().getClass());
        return isUpdatingExistingPublication || isUpdatingNewPublication;
    }

    private void setRrsOnAllFiles(Resource resourceUpdate, CustomerApiRightsRetention rrsConfig,
                                  String actingUser) throws BadRequestException, UnauthorizedException {
        var rrsValueFinder = new RightsRetentionsValueFinder(rrsConfig,
                                                             permissionStrategy,
                                                             actingUser);
        var files = Lists.newArrayList(resourceUpdate.getAssociatedArtifacts()).stream()
                        .filter(File.class::isInstance)
                        .map(File.class::cast)
                        .toList();

        for (var file : files) {
            setRrsOnFile(file, resourceUpdate, rrsValueFinder);
        }
    }

    private void setRrsOnModifiedFiles(Resource resourceUpdate,
                                       Resource existingResource,
                                       CustomerApiRightsRetention rrsConfig,
                                       String actingUser)
        throws BadRequestException, UnauthorizedException {
        var filesOnUpdateRequest = getFilesFromResoure(resourceUpdate);
        var filesOnExistingPublication = getFilesFromResoure(existingResource);

        var newFiles =
            filesOnUpdateRequest.values().stream()
                .filter(file -> !filesOnExistingPublication.containsKey(file.getIdentifier()))
                .toList();

        var modifiedFiles = filesOnUpdateRequest.values().stream()
                                .filter(file -> filesOnExistingPublication.containsKey(file.getIdentifier()))
                                .filter(file -> !file.equals(filesOnExistingPublication.get(file.getIdentifier())))
                                .toList();
        var rrsValueFinder = new RightsRetentionsValueFinder(rrsConfig,
                                                             permissionStrategy,
                                                             actingUser);
        for (File newFile : newFiles) {
            setRrsOnFile(newFile, resourceUpdate, rrsValueFinder);
        }
        for (File newFile : modifiedFiles) {
            var oldFile = filesOnExistingPublication.get(newFile.getIdentifier());
            setRrsOnModifiedFile(newFile, oldFile, resourceUpdate, rrsValueFinder);
        }
    }

    private void setRrsOnModifiedFile(File file, File oldFile,
                                      Resource resource,
                                      RightsRetentionsValueFinder rrsValueFinder)
        throws BadRequestException, UnauthorizedException {
        if (!file.getRightsRetentionStrategy().getClass().equals(oldFile.getRightsRetentionStrategy().getClass())) {
            file.setRightsRetentionStrategy(rrsValueFinder.getRightsRetentionStrategy(file, resource));
        } else {
            file.setRightsRetentionStrategy(oldFile.getRightsRetentionStrategy());
        }
    }

    private void setRrsOnFile(File file, Resource resource,
                              RightsRetentionsValueFinder rrsValueFinder)
        throws BadRequestException, UnauthorizedException {
        file.setRightsRetentionStrategy(rrsValueFinder.getRightsRetentionStrategy(file, resource));
    }

    private static Map<UUID, File> getFilesFromResoure(Resource resource) {
        return Lists.newArrayList(resource.getFiles()).stream()
                   .collect(Collectors.toMap(File::getIdentifier, Function.identity()));
    }


    public static RightsRetentionsApplier rrsApplierForNewPublication(Resource resource,
                                                                      CustomerApiRightsRetention customerRrs,
                                                                      String actingUser) {
        return new RightsRetentionsApplier(Optional.empty(),
                                           resource,
                                           customerRrs,
                                           actingUser,
                                           null);
    }

    public static RightsRetentionsApplier rrsApplierForUpdatedPublication(Resource existingResource,
                                                                          Resource updatedResource,
                                                                          CustomerApiRightsRetention customerRrs,
                                                                          String actingUser,
                                                                          PublicationPermissions strategy) {
        return new RightsRetentionsApplier(Optional.of(existingResource),
                                           updatedResource,
                                           customerRrs,
                                           actingUser,
                                           strategy);
    }


}
