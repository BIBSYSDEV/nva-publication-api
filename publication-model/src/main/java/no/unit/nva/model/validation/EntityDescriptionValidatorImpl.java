package no.unit.nva.model.validation;

import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.contexttypes.PublicationContext;
import nva.commons.core.paths.UriWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

public class EntityDescriptionValidatorImpl implements EntityDescriptionValidator {

    private static final String UNSYNCHRONIZED_PUBLICATION_CHANNEL_DATE_MESSAGE =
            "EntityDescription contains unsynchronized publication-date and publication-channel combinations";
    private static final String POST_SECOND_MILLENNIUM_YEAR_REGEX = "[0-9]{4}";
    private static List<String> errors;

    @Override
    public ValidationReport validate(EntityDescription entityDescription) {
        errors = new ArrayList<>();
        var publicationContext = entityDescription.getReference().getPublicationContext();
        var publicationDate = entityDescription.getPublicationDate();
        if (hasUnsynchronizedPublicationDateChannelDatePair(publicationContext, publicationDate)) {
            errors.add(UNSYNCHRONIZED_PUBLICATION_CHANNEL_DATE_MESSAGE);
        }
        return new EntityDescriptionValidationReport(errors);
    }

    private boolean hasUnsynchronizedPublicationDateChannelDatePair(PublicationContext context,
                                                                    PublicationDate publicationDate) {
        return nonNull(context) && hasInvalidChannelYears(context, publicationDate);

    }

    private boolean hasInvalidChannelYears(PublicationContext context, PublicationDate publicationDate) {
        var channelYears = extractPublicationChannelYears(context);
        return !channelYears.isEmpty() && (hasMultipleChannelYears(channelYears)
                || hasMismatchedPublicationDate(channelYears.getFirst(), publicationDate));
    }

    private boolean hasMultipleChannelYears(List<String> channelYears) {
        return channelYears.size() > 1;
    }

    private boolean hasMismatchedPublicationDate(String channelYear, PublicationDate publicationDate) {
        return Optional.ofNullable(publicationDate)
                .map(PublicationDate::getYear)
                .map(publicationYear -> !publicationYear.equals(channelYear))
                .orElse(true);
    }

    private static List<String> extractPublicationChannelYears(PublicationContext context) {
        return context.extractPublicationContextUris().stream()
                .map(UriWrapper::fromUri)
                .map(EntityDescriptionValidatorImpl::extractPublicationChannelYear)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();
    }

    public static Optional<String> extractPublicationChannelYear(UriWrapper uriWrapper) {
        var year = attempt(uriWrapper::getLastPathElement).stream()
                .filter(EntityDescriptionValidatorImpl::isValidYear)
                .findFirst();
        if (year.isPresent()) {
            return year;
        } else {
            errors.add("Publication channel URI is malformed: " + uriWrapper);
            return Optional.empty();
        }
    }

    private static boolean isValidYear(String candidate) {
        return !candidate.isBlank() && candidate.matches(POST_SECOND_MILLENNIUM_YEAR_REGEX);
    }
}
