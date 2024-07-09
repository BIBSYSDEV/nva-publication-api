package no.unit.nva.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static nva.commons.core.attempt.Try.attempt;
import java.time.Year;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import no.unit.nva.model.instancetypes.PublicationInstance;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

/* default */ final class FindableDoiRequirementsValidator {

    private static final Set<PublicationStatus> VALID_PUBLICATION_STATUS_FOR_FINDABLE_DOI =
        Set.of(PUBLISHED, PUBLISHED_METADATA);
    public static final int PUBLISH_YEAR_MIN = 1000;
    public static final int YEARS_AHEAD_ASSUMED_FROM_DOCS = 4;
    public static final IntSupplier PUBLISH_YEAR_MAX = () -> Year.now().getValue() + YEARS_AHEAD_ASSUMED_FROM_DOCS;

    @JacocoGenerated
    private FindableDoiRequirementsValidator() {

    }

    /* default */
    static boolean meetsFindableDoiRequirements(Publication publication) {
        return hasCorrectPublishedStatus(publication) &&
               mandatoryFieldsAreNotNull(publication) &&
               hasValidPublishedYear(publication);
    }

    private static boolean hasValidPublishedYear(Publication publication) {
        //  docs: https://support.datacite.org/docs/field-descriptions-for-form#publication-year
        var yearString = publication.getEntityDescription().getPublicationDate().getYear();
        var year = attempt(() -> Integer.parseInt(yearString)).toOptional().orElse(null);
        return nonNull(year) && year >= PUBLISH_YEAR_MIN && year <= PUBLISH_YEAR_MAX.getAsInt();
    }

    private static boolean hasCorrectPublishedStatus(Publication publication) {
        return VALID_PUBLICATION_STATUS_FOR_FINDABLE_DOI.contains(publication.getStatus());
    }

    private static boolean mandatoryFieldsAreNotNull(Publication publication) {
        return nonNull(publication.getIdentifier())
               && nonNull(publication.getPublisher())
               && nonNull(publication.getPublisher().getId())
               && nonNull(publication.getModifiedDate())
               && hasAMainTitle(publication)
               && hasAnInstanceType(publication)
               && hasPublicationYear(publication);
    }

    private static boolean hasPublicationYear(Publication publication) {
        return Optional.ofNullable(publication.getEntityDescription())
                   .map(EntityDescription::getPublicationDate)
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
