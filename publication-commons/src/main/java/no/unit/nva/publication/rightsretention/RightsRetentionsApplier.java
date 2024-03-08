package no.unit.nva.publication.rightsretention;

import static no.unit.nva.publication.rightsretention.RightsRetentionsValueFinder.getRightsRetentionStrategy;
import com.google.common.collect.Lists;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import nva.commons.apigateway.exceptions.BadRequestException;

public class RightsRetentionsApplier {

    private Optional<Publication> existingPublication;
    private Publication updatedPublication;
    private CustomerApiRightsRetention configuredRrsOnCustomer;
    private String actingUser;

    private RightsRetentionsApplier(Optional<Publication> existingPublication,
                                    Publication updatedPublication,
                                    CustomerApiRightsRetention configuredRrsOnCustomer,
                                    String actingUser) {
        this.existingPublication = existingPublication;
        this.updatedPublication = updatedPublication;
        this.configuredRrsOnCustomer = configuredRrsOnCustomer;
        this.actingUser = actingUser;
    }

    public void handle() throws BadRequestException {
        if (existingPublication.isEmpty() || isChangedPublicationType(updatedPublication, existingPublication.get())) {
            setRrsOnAllFiles(updatedPublication, configuredRrsOnCustomer, actingUser);
        } else {
            setRrsOnModifiedFiles(updatedPublication, existingPublication.get(), configuredRrsOnCustomer,
                                  actingUser);
        }
    }

    private static boolean isChangedPublicationType(Publication publicationUpdate, Publication existingPublication) {
        PublicationContext newContext = publicationUpdate.getEntityDescription()
                                            .getReference()
                                            .getPublicationContext();
        PublicationContext existingContext = existingPublication.getEntityDescription()
                                                 .getReference()
                                                 .getPublicationContext();
        return Objects.nonNull(newContext)
             && Objects.nonNull(existingContext)
             && !newContext.getClass().equals(existingContext.getClass());
    }

    private void setRrsOnAllFiles(Publication publicationUpdate, CustomerApiRightsRetention rrsConfig,
                                  String actingUser) throws BadRequestException {
        var files = Lists.newArrayList(publicationUpdate.getAssociatedArtifacts()).stream()
                        .filter(File.class::isInstance)
                        .map(File.class::cast)
                        .toList();

        for (var file : files) {
            setRrsOnFile(file, publicationUpdate, rrsConfig, actingUser);
        }
    }

    private void setRrsOnModifiedFiles(Publication publicationUpdate, Publication existingPublication,
                                       CustomerApiRightsRetention rrsConfig,
                                       String actingUser) throws BadRequestException {
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

        for (File newFile : newFiles) {
            setRrsOnFile(newFile, publicationUpdate, rrsConfig, actingUser);
        }
        for (File newFile : modifiedFiles) {
            var oldFile = filesOnExistingPublication.get(newFile.getIdentifier());
            setRrsOnModifiedFile(newFile, oldFile, publicationUpdate, rrsConfig, actingUser);
        }
    }

    private void setRrsOnModifiedFile(File file, File oldFile, Publication publication, CustomerApiRightsRetention rrs,
                                      String actingUser)
        throws BadRequestException {
        if (!file.getRightsRetentionStrategy().getClass().equals(oldFile.getRightsRetentionStrategy().getClass())) {
            file.setRightsRetentionStrategy(getRightsRetentionStrategy(rrs, publication, file, actingUser));
        } else {
            file.setRightsRetentionStrategy(oldFile.getRightsRetentionStrategy());
        }
    }

    private void setRrsOnFile(File file, Publication publication, CustomerApiRightsRetention rrs, String actingUser)
        throws BadRequestException {
        file.setRightsRetentionStrategy(getRightsRetentionStrategy(rrs, publication, file, actingUser));
    }

    private static Map<UUID, File> getFilesFromPublication(Publication publicationUpdate) {
        return Lists.newArrayList(publicationUpdate.getAssociatedArtifacts()).stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast).collect(Collectors.toMap(File::getIdentifier, Function.identity()));
    }


    public static RightsRetentionsApplier rrsApplierForNewPublication(Publication newPublication,
                                                                      CustomerApiRightsRetention configuredRrsOnCustomer,
                                                                      String actingUser) {
        return new RightsRetentionsApplier(Optional.empty(), newPublication, configuredRrsOnCustomer, actingUser);
    }

    public static RightsRetentionsApplier rrsApplierForUpdatedPublication(Publication existingPublication,
                                                                      Publication updatedPublication,
                                                                      CustomerApiRightsRetention configuredRrsOnCustomer,
                                                                      String actingUser) {
        return new RightsRetentionsApplier(Optional.of(existingPublication),  updatedPublication, configuredRrsOnCustomer, actingUser);
    }


}
