package no.unit.nva.publication.validation;

import java.net.URI;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;

public class DefaultPublicationValidator implements PublicationValidator {

    private final FilesAllowedForTypesSupplier filesAllowedForTypesSupplier;

    public DefaultPublicationValidator(final FilesAllowedForTypesSupplier filesAllowedForTypesSupplier) {
        this.filesAllowedForTypesSupplier = filesAllowedForTypesSupplier;
    }

    @Override
    public void validate(Publication publication, URI customerUri) throws PublicationValidationException {
        if (publication.getEntityDescription() != null) {
            var hasFiles = hasAtLeastOneFile(publication);
            validate(publication.getEntityDescription(), hasFiles, customerUri);
        }
    }

    private void validate(EntityDescription entityDescription, boolean hasFiles, URI customerUri)
        throws PublicationValidationException {
        var instanceType = getInstanceType(entityDescription);
        if (instanceType != null && hasFiles && !filesAllowedForTypesSupplier.get(customerUri).contains(
            getInstanceType(entityDescription))) {
            throw new PublicationValidationException(
                String.format("Files not allowed for instance type %s",
                              entityDescription
                                  .getReference()
                                  .getPublicationInstance()
                                  .getInstanceType()));
        }
    }

    private static String getInstanceType(EntityDescription entityDescription) {
        if (entityDescription.getReference() != null
            && entityDescription.getReference().getPublicationInstance() != null) {
            return entityDescription
                       .getReference()
                       .getPublicationInstance()
                       .getInstanceType();
        } else {
            return null;
        }
    }

    private boolean hasAtLeastOneFile(Publication publication) {
        return publication.getAssociatedArtifacts() != null
               &&
               publication.getAssociatedArtifacts().stream()
                   .anyMatch(
                       associatedArtifact -> associatedArtifact instanceof File);
    }
}
