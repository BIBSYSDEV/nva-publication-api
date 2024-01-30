package no.unit.nva.publication.validation;

import static java.util.Objects.nonNull;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.Customer;

public class DefaultPublicationValidator implements PublicationValidator {

    public static final String FILES_NOT_ALLOWED_MESSAGE = "Files not allowed for instance type %s";

    public DefaultPublicationValidator() {
    }

    @Override
    public void validate(Publication publication, Customer customer)
        throws PublicationValidationException {
        if (nonNull(publication.getEntityDescription())) {
            var hasFiles = hasAtLeastOneFile(publication);
            validate(publication.getEntityDescription(), hasFiles, customer);
        }
    }

    private void validate(EntityDescription entityDescription, boolean hasFiles, Customer customer)
        throws PublicationValidationException {
        var instanceType = getInstanceType(entityDescription);
        if (nonNull(instanceType)
            &&
            hasFiles
            &&
            !customer.getAllowFileUploadForTypes().contains(getInstanceType(entityDescription))) {
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
