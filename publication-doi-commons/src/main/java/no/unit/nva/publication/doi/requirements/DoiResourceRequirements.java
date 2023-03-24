package no.unit.nva.publication.doi.requirements;

import static java.util.Objects.nonNull;
import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public final class DoiResourceRequirements {

    @JacocoGenerated
    private DoiResourceRequirements() {

    }

    public static boolean publicationSatisfiesDoiRequirements(Publication publication) {
        return hasCorrectPublishedStatus(publication) && mandatoryFieldsAreNotNull(publication);
    }

    private static boolean hasCorrectPublishedStatus(Publication newEntry) {
        return PublicationStatus.PUBLISHED.equals(newEntry.getStatus())
               || PublicationStatus.PUBLISHED_METADATA.equals(newEntry.getStatus());
    }

    private static boolean mandatoryFieldsAreNotNull(Publication publication) {
        return nonNull(publication.getIdentifier())
               && nonNull(publication.getPublisher())
               && nonNull(publication.getPublisher().getId())
               && nonNull(publication.getModifiedDate())
               && hasAMainTitle(publication)
               && hasAnInstanceType(publication)
               && hasADate(publication);
    }

    private static boolean hasADate(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getDate)
                   .map(PublicationDate::getYear)
                   .isPresent();
    }

    private static boolean hasAnInstanceType(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(PublicationInstance::getInstanceType)
                   .isPresent();
    }

    private static boolean hasAMainTitle(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getMainTitle)
                   .map(StringUtils::isNotEmpty)
                   .orElse(false);
    }
}
