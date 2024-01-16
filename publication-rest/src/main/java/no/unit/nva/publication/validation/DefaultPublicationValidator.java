package no.unit.nva.publication.validation;

import static java.util.Objects.nonNull;
import java.net.URI;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;

public class DefaultPublicationValidator implements PublicationValidator {

    public static final String FILES_NOT_ALLOWED_MESSAGE = "Files not allowed for instance type %s";
    private final FilesAllowedForTypesSupplier filesAllowedForTypesSupplier;

    public DefaultPublicationValidator(final FilesAllowedForTypesSupplier filesAllowedForTypesSupplier) {
        this.filesAllowedForTypesSupplier = filesAllowedForTypesSupplier;
    }

    @Override
    public void validate(Publication publication, URI customerUri) throws PublicationValidationException {
        if (nonNull(publication.getEntityDescription())) {
            var hasFiles = hasAtLeastOneFile(publication);
            validate(publication.getEntityDescription(), hasFiles, customerUri);
        }
    }

    private void validate(EntityDescription entityDescription, boolean hasFiles, URI customerUri)
        throws PublicationValidationException {
        var instanceType = getInstanceType(entityDescription);
        if (nonNull(instanceType) && hasFiles && !filesAllowedForTypesSupplier.get(customerUri).contains(
            getInstanceType(entityDescription))) {
            throw new PublicationValidationException(String.format(FILES_NOT_ALLOWED_MESSAGE, instanceType));
        }
    }

    private static String getInstanceType(EntityDescription entityDescription) {
        if (nonNull(entityDescription.getReference())
            && nonNull(entityDescription.getReference().getPublicationInstance())) {
            return entityDescription
                       .getReference()
                       .getPublicationInstance()
                       .getInstanceType();
        } else {
            return null;
        }
    }

    private boolean hasAtLeastOneFile(Publication publication) {
        return nonNull(publication.getAssociatedArtifacts())
               &&
               publication.getAssociatedArtifacts().stream().anyMatch(File.class::isInstance);
    }
}
