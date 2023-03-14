package no.unit.nva.publication.events.handlers.tickets;

import static java.util.Objects.nonNull;
import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public final class DoiResourceRequirements {

    @JacocoGenerated
    private DoiResourceRequirements() {

    }

    public static boolean publicationSatisfiesDoiRequirements(DoiRequest newEntry, ResourceService resourceService) {
        return publicationSatisfiesDoiRequirements(Resource.fromPublication(newEntry.toPublication(resourceService)));
    }

    public static boolean publicationSatisfiesDoiRequirements(Resource newEntry) {
        return hasCorrectPublishedStatus(newEntry) && mandatoryFieldsAreNotNull(newEntry);
    }

    private static boolean hasCorrectPublishedStatus(Resource newEntry) {
        return PublicationStatus.PUBLISHED.equals(newEntry.getStatus())
               || PublicationStatus.PUBLISHED_METADATA.equals(newEntry.getStatus());
    }

    private static boolean mandatoryFieldsAreNotNull(Resource newEntry) {
        return nonNull(newEntry.getIdentifier())
               && nonNull(newEntry.getPublisher())
               && nonNull(newEntry.getModifiedDate())
               && hasAMainTitle(newEntry)
               && hasAnInstanceType(newEntry)
               && hasADate(newEntry);
    }

    private static boolean hasADate(Resource newEntry) {
        return Optional.ofNullable(newEntry.getEntityDescription())
                   .map(EntityDescription::getDate)
                   .map(PublicationDate::getYear)
                   .isPresent();
    }

    private static boolean hasAnInstanceType(Resource newEntry) {
        return Optional.ofNullable(newEntry.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .map(PublicationInstance::getInstanceType)
                   .isPresent();
    }

    private static boolean hasAMainTitle(Resource newEntry) {
        return Optional.ofNullable(newEntry.getEntityDescription())
                   .map(EntityDescription::getMainTitle)
                   .map(StringUtils::isNotEmpty)
                   .orElse(false);
    }
}
