package no.unit.nva.publication.rightsretention;

import com.google.common.collect.Lists;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.publication.permissions.publication.PublicationPermissionStrategy;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class RightsRetentionsApplier {

    private final Optional<Publication> existingPublication;
    private final Publication updatedPublication;
    private final CustomerApiRightsRetention configuredRrsOnCustomer;
    private final String actingUser;
    private final PublicationPermissionStrategy permissionStrategy;

    private RightsRetentionsApplier(Optional<Publication> existingPublication,
                                    Publication updatedPublication,
                                    CustomerApiRightsRetention configuredRrsOnCustomer,
                                    String actingUser,
                                    PublicationPermissionStrategy permissionStrategy) {
        this.existingPublication = existingPublication;
        this.updatedPublication = updatedPublication;
        this.configuredRrsOnCustomer = configuredRrsOnCustomer;
        this.actingUser = actingUser;
        this.permissionStrategy = permissionStrategy;
    }

    public void handle() throws BadRequestException, UnauthorizedException {
        if (existingPublication.isEmpty() || isChangedPublicationType(updatedPublication, existingPublication.get())) {
            setRrsOnAllFiles(updatedPublication, configuredRrsOnCustomer, actingUser);
        } else {
            setRrsOnModifiedFiles(updatedPublication,
                                  existingPublication.get(),
                                  configuredRrsOnCustomer,
                                  actingUser);
        }
    }

    private static boolean isChangedPublicationType(Publication publicationUpdate, Publication existingPublication) {
        var newInstance = Optional.ofNullable(publicationUpdate)
                              .map(Publication::getEntityDescription)
                              .map(EntityDescription::getReference)
                              .map(Reference::getPublicationInstance);
        var existingInstance = Optional.ofNullable(existingPublication)
                                   .map(Publication::getEntityDescription)
                                   .map(EntityDescription::getReference)
                                   .map(Reference::getPublicationInstance);
        var isUpdatingNewPublication = existingInstance.isEmpty() && newInstance.isPresent();
        var isUpdatingExistingPublication = existingInstance.isPresent()
                                            && newInstance.isPresent()
                                            && !newInstance.get().getClass().equals(existingInstance.get().getClass());
        return isUpdatingExistingPublication || isUpdatingNewPublication;
    }

    private void setRrsOnAllFiles(Publication publicationUpdate, CustomerApiRightsRetention rrsConfig,
                                  String actingUser) throws BadRequestException, UnauthorizedException {
        var rrsValueFinder = new RightsRetentionsValueFinder(rrsConfig,
                                                             permissionStrategy,
                                                             actingUser);
        var files = Lists.newArrayList(publicationUpdate.getAssociatedArtifacts()).stream()
                        .filter(File.class::isInstance)
                        .map(File.class::cast)
                        .toList();

        for (var file : files) {
            setRrsOnFile(file, publicationUpdate, rrsValueFinder);
        }
    }

    private void setRrsOnModifiedFiles(Publication publicationUpdate,
                                       Publication existingPublication,
                                       CustomerApiRightsRetention rrsConfig,
                                       String actingUser)
        throws BadRequestException, UnauthorizedException {
        var filesOnUpdateRequest = getFilesFromPublication(publicationUpdate);
        var filesOnExistingPublication = getFilesFromPublication(existingPublication);

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
            setRrsOnFile(newFile, publicationUpdate, rrsValueFinder);
        }
        for (File newFile : modifiedFiles) {
            var oldFile = filesOnExistingPublication.get(newFile.getIdentifier());
            setRrsOnModifiedFile(newFile, oldFile, publicationUpdate, rrsValueFinder);
        }
    }

    private void setRrsOnModifiedFile(File file, File oldFile,
                                      Publication publication,
                                      RightsRetentionsValueFinder rrsValueFinder)
        throws BadRequestException, UnauthorizedException {
        if (!file.getRightsRetentionStrategy().getClass().equals(oldFile.getRightsRetentionStrategy().getClass())) {
            file.setRightsRetentionStrategy(rrsValueFinder.getRightsRetentionStrategy(file, publication));
        } else {
            file.setRightsRetentionStrategy(oldFile.getRightsRetentionStrategy());
        }
    }

    private void setRrsOnFile(File file, Publication publication,
                              RightsRetentionsValueFinder rrsValueFinder)
        throws BadRequestException, UnauthorizedException {
        file.setRightsRetentionStrategy(rrsValueFinder.getRightsRetentionStrategy(file, publication));
    }

    private static Map<UUID, File> getFilesFromPublication(Publication publicationUpdate) {
        return Lists.newArrayList(publicationUpdate.getAssociatedArtifacts()).stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast).collect(Collectors.toMap(File::getIdentifier, Function.identity()));
    }


    public static RightsRetentionsApplier rrsApplierForNewPublication(Publication newPublication,
                                                                      CustomerApiRightsRetention customerRrs,
                                                                      String actingUser) {
        return new RightsRetentionsApplier(Optional.empty(),
                                           newPublication,
                                           customerRrs,
                                           actingUser,
                                           null);
    }

    public static RightsRetentionsApplier rrsApplierForUpdatedPublication(Publication existingPublication,
                                                                          Publication updatedPublication,
                                                                          CustomerApiRightsRetention customerRrs,
                                                                          String actingUser,
                                                                          PublicationPermissionStrategy strategy) {
        return new RightsRetentionsApplier(Optional.of(existingPublication),
                                           updatedPublication,
                                           customerRrs,
                                           actingUser,
                                           strategy);
    }


}
