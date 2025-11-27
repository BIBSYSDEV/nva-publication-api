package no.unit.nva.model.validation;

import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

public class EntityDescriptionValidatorImpl implements EntityDescriptionValidator {

    private static final String CONTEXT_UNSYNCHRONIZED_PUBLICATION_CHANNEL_DATE_MESSAGE =
            "EntityDescription PublicationContext contains unsynchronized publication-date "
                    + "and publication-channel combinations";
    private static final String INSTANCE_UNSYNCHRONIZED_PUBLICATION_CHANNEL_DATE_MESSAGE =
            "EntityDescription PublicationInstance contains unsynchronized publication-date "
                    + "and publication-channel combinations";
    private static final String POST_SECOND_MILLENNIUM_YEAR_REGEX = "[0-9]{4}";
    private static List<String> errors;

    @Override
    public ValidationReport validate(EntityDescription entityDescription) {
        errors = new ArrayList<>();
        var reference = entityDescription.getReference();
        var publicationContext = reference.getPublicationContext();
        var publicationDate = entityDescription.getPublicationDate();
        if (hasUnsynchronizedPublicationDateChannelDatePair(publicationContext.extractPublicationContextUris(),
                publicationDate)) {
            errors.add(CONTEXT_UNSYNCHRONIZED_PUBLICATION_CHANNEL_DATE_MESSAGE);
        }
        var publicationInstance = reference.getPublicationInstance();
        if (hasUnsynchronizedPublicationDateChannelDatePair(publicationInstance.extractPublicationContextUris(),
                publicationDate)) {
            errors.add(INSTANCE_UNSYNCHRONIZED_PUBLICATION_CHANNEL_DATE_MESSAGE);
        }
        return new EntityDescriptionValidationReport(errors);
    }

    private boolean hasUnsynchronizedPublicationDateChannelDatePair(Collection<URI> uris,
                                                                    PublicationDate publicationDate) {
        return nonNull(uris) && hasInvalidChannelYears(uris, publicationDate);

    }

    private boolean hasInvalidChannelYears(Collection<URI> uris, PublicationDate publicationDate) {
        var channelYears = extractPublicationChannelYears(uris);
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

    private static List<String> extractPublicationChannelYears(Collection<URI> uris) {
        return uris.stream()
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
